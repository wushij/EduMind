package com.edumind.teaching.service.query.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.query.SubmissionStatsQueryService;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作业提交与成绩只读统计。
 *
 * <p><b>性能约定</b>：本类是全平台被调用最频繁的统计入口之一
 * （学情分析、教学报告、知识掌握度、推荐服务都会间接调用），
 * 因此所有统计必须基于<b>一次批量答卷扫描</b>完成，禁止按作业逐条查询。</p>
 *
 * <p>历史实现存在两处放大：</p>
 * <ol>
 *   <li>{@code for (assignment) submissionDao.listByAssignmentId(...)} —— 一节课 20 份作业就是 20 次查询，
 *       而教学报告一次请求会调用本方法 4 次（1 次直接 + 3 次经推荐/掌握度间接），放大成 80+ 次往返；</li>
 *   <li>课程级汇总与学生维度明细各扫一遍全量答卷，两份数据本可一次产出。</li>
 * </ol>
 *
 * @see #loadCourseSubmissionData(Long) 统一数据加载入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionStatsQueryServiceImpl implements SubmissionStatsQueryService {

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final CourseQueryApi courseQueryApi;

    /**
     * 课程作业提交统计。
     *
     * <p>提交率口径：应提交槽位 = Σ(每份已发布/已关闭作业的选课学生数)，草稿作业不计入分母；
     * 分子只统计选课学生的提交，避免非选课账号（教务试做、教师代交）占满槽位导致 100% 失真。</p>
     */
    @Override
    public SubmissionStatsVO getCourseSubmissionStats(Long courseId) {
        CourseSubmissionData data = loadCourseSubmissionData(courseId);
        SubmissionStatsVO stats = new SubmissionStatsVO();
        stats.setCourseId(courseId);
        stats.setTotalAssignments(data.assignments.size());
        stats.setSubmittedCount(data.submittedCount);
        stats.setGradedCount(data.gradedCount);
        stats.setExpectedSubmissionCount(data.expectedSlots);
        double rate = data.expectedSlots > 0 ? data.submittedSlots * 100.0 / data.expectedSlots : 0;
        stats.setAvgSubmissionRate(Math.min(100.0, rate));
        stats.setAvgScore(data.scoreCount > 0 ? data.scoreSum / data.scoreCount : 0);
        // 学生明细与课程汇总同源同批产出，调用方无需再次全量扫描
        stats.setStudentScores(data.studentScores);
        return stats;
    }

    @Override
    public List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId) {
        return loadCourseSubmissionData(courseId).studentScores;
    }

    @Override
    public List<SubmissionStatsVO.DailyScoreVO> listDailyAvgScores(Long courseId, LocalDate startDate, LocalDate endDate) {
        List<Long> assignmentIds = null;
        if (courseId != null) {
            assignmentIds = assignmentDao.listByCourseId(courseId).stream()
                    .map(AssignmentEntity::getId)
                    .collect(Collectors.toList());
        }
        // courseId 为 null 时聚合全平台提交，作为"全校对照"基准线；
        // 传空集合会命中 DAO 的 -1 兜底，因此这里保持 null 语义而非空列表。
        Map<LocalDate, double[]> dailyAgg = new LinkedHashMap<>();
        for (SubmissionEntity sub : submissionDao.listByScope(assignmentIds, null, null, null, null, null)) {
            if (sub.getSubmitTime() == null || !hasUsableScore(sub)) {
                continue;
            }
            LocalDate day = sub.getSubmitTime().toLocalDate();
            if (startDate != null && day.isBefore(startDate)) {
                continue;
            }
            if (endDate != null && day.isAfter(endDate)) {
                continue;
            }
            double[] cell = dailyAgg.computeIfAbsent(day, k -> new double[2]);
            cell[0] += sub.getTotalScore() * 100.0 / sub.getMaxScore();
            cell[1] += 1;
        }

        return dailyAgg.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    SubmissionStatsVO.DailyScoreVO vo = new SubmissionStatsVO.DailyScoreVO();
                    vo.setDate(e.getKey().toString());
                    vo.setAvgScore(Math.round(e.getValue()[0] / e.getValue()[1] * 10.0) / 10.0);
                    vo.setSampleCount((int) e.getValue()[1]);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 课程答卷数据统一加载入口：一次取作业列表 + 一次批量取答卷，
     * 在内存中一次性算出课程级汇总与学生维度明细。
     */
    private CourseSubmissionData loadCourseSubmissionData(Long courseId) {
        CourseSubmissionData data = new CourseSubmissionData();
        if (courseId == null) {
            return data;
        }
        List<AssignmentEntity> assignments = assignmentDao.listByCourseId(courseId);
        data.assignments = assignments != null ? assignments : new ArrayList<>();
        if (data.assignments.isEmpty()) {
            return data;
        }

        List<Long> assignmentIds = data.assignments.stream()
                .map(AssignmentEntity::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 应提交槽位基数：课程选课学生数
        Set<Long> enrolledStudentSet = new HashSet<>(resolveCourseStudentIds(courseId));
        int courseStudentCount = enrolledStudentSet.size();

        // 单次批量拉取该课程全部答卷（替代按作业逐条查询）
        List<SubmissionEntity> submissions = submissionDao.listByScope(assignmentIds, null, null, null, null, null);

        Map<Long, Integer> submittedSlotsByAssignment = new HashMap<>();
        Map<Long, Integer> submissionCountByStudent = new HashMap<>();
        Map<Long, double[]> scoreAggByStudent = new HashMap<>();

        for (SubmissionEntity sub : submissions) {
            boolean usableScore = hasUsableScore(sub);
            if (isSubmitted(sub.getStatus())) {
                data.submittedCount++;
                // 只有选课学生的提交才算占用了一个应提交槽位
                if (sub.getStudentId() != null && enrolledStudentSet.contains(sub.getStudentId())) {
                    submittedSlotsByAssignment.merge(sub.getAssignmentId(), 1, Integer::sum);
                }
            }
            if (usableScore) {
                data.gradedCount++;
                data.scoreSum += sub.getTotalScore() * 100.0 / sub.getMaxScore();
                data.scoreCount++;
            }
            if (sub.getStudentId() != null) {
                submissionCountByStudent.merge(sub.getStudentId(), 1, Integer::sum);
                if (usableScore) {
                    double[] cell = scoreAggByStudent.computeIfAbsent(sub.getStudentId(), k -> new double[2]);
                    cell[0] += sub.getTotalScore() * 100.0 / sub.getMaxScore();
                    cell[1] += 1;
                }
            }
        }

        // 仅已发布/已关闭作业需要学生提交，草稿作业不计入应提交槽位
        for (AssignmentEntity assignment : data.assignments) {
            if (isOpenForSubmission(assignment.getStatus())) {
                data.submittedSlots += submittedSlotsByAssignment.getOrDefault(assignment.getId(), 0);
                data.expectedSlots += Math.max(courseStudentCount, 1);
            }
        }

        for (Map.Entry<Long, Integer> entry : submissionCountByStudent.entrySet()) {
            SubmissionStatsVO.StudentScoreVO vo = new SubmissionStatsVO.StudentScoreVO();
            vo.setStudentId(entry.getKey());
            vo.setSubmissionCount(entry.getValue());
            double[] cell = scoreAggByStudent.get(entry.getKey());
            vo.setAvgScore(cell != null && cell[1] > 0
                    ? Math.round(cell[0] / cell[1] * 10.0) / 10.0
                    : 0.0);
            data.studentScores.add(vo);
        }
        return data;
    }

    /** 课程选课学生名单；查询失败时返回空集合（每份作业兜底 1 个槽位，提交率记 0） */
    private List<Long> resolveCourseStudentIds(Long courseId) {
        if (courseId == null) {
            return new ArrayList<>();
        }
        try {
            List<Long> studentIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
            return studentIds != null ? studentIds : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to resolve student count for course {}: {}", courseId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 学生可见且需要提交的作业状态：已发布 / 已关闭（草稿、归档对分子分母都无意义） */
    private static boolean isOpenForSubmission(String status) {
        return "PUBLISHED".equals(status) || "CLOSED".equals(status);
    }

    /**
     * 已提交：SUBMITTED（待批改）/ GRADED（AI 已评）/ REVIEWED（教师已终审）。
     * 教师终审后的答卷不应从"已提交"里消失——早期漏算 REVIEWED，导致课程统计偏低。
     */
    private static boolean isSubmitted(String status) {
        return "SUBMITTED".equals(status) || "GRADED".equals(status) || "REVIEWED".equals(status);
    }

    /** 答卷是否具备可纳入均分计算的分数（已批改且满分有效） */
    private static boolean hasUsableScore(SubmissionEntity sub) {
        return sub.getTotalScore() != null
                && sub.getMaxScore() != null
                && sub.getMaxScore() > 0
                && ("GRADED".equals(sub.getStatus()) || "REVIEWED".equals(sub.getStatus()));
    }

    /** 一次扫描产出的课程答卷统计中间结果 */
    private static final class CourseSubmissionData {
        private List<AssignmentEntity> assignments = new ArrayList<>();
        private int submittedCount;
        private int gradedCount;
        private int submittedSlots;
        private int expectedSlots;
        private double scoreSum;
        private int scoreCount;
        private final List<SubmissionStatsVO.StudentScoreVO> studentScores = new ArrayList<>();
    }
}
