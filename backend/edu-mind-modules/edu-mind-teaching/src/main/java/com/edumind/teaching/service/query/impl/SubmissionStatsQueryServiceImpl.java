package com.edumind.teaching.service.query.impl;

import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.query.SubmissionStatsQueryService;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionStatsQueryServiceImpl implements SubmissionStatsQueryService {

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;

    @Override
    public SubmissionStatsVO getCourseSubmissionStats(Long courseId) {
        SubmissionStatsVO stats = new SubmissionStatsVO();
        stats.setCourseId(courseId);
        List<AssignmentEntity> assignments = assignmentDao.listByCourseId(courseId);
        stats.setTotalAssignments(assignments.size());
        int submitted = 0;
        int graded = 0;
        double scoreSum = 0;
        int scoreCount = 0;
        for (AssignmentEntity assignment : assignments) {
            List<SubmissionEntity> subs = submissionDao.listByAssignmentId(assignment.getId());
            for (SubmissionEntity sub : subs) {
                if (isSubmitted(sub.getStatus())) {
                    submitted++;
                }
                if (isGraded(sub.getStatus()) && sub.getTotalScore() != null && sub.getMaxScore() != null
                        && sub.getMaxScore() > 0) {
                    graded++;
                    scoreSum += sub.getTotalScore() * 100.0 / sub.getMaxScore();
                    scoreCount++;
                }
            }
        }
        stats.setSubmittedCount(submitted);
        stats.setGradedCount(graded);
        int expected = assignments.size() > 0 ? assignments.size() * 2 : 0;
        stats.setAvgSubmissionRate(expected > 0 ? submitted * 100.0 / expected : 0);
        stats.setAvgScore(scoreCount > 0 ? scoreSum / scoreCount : 0);
        return stats;
    }

    /**
     * 已提交：SUBMITTED（待批改）/ GRADED（AI 已评）/ REVIEWED（教师已终审）。
     * 教师终审后的答卷不应从"已提交"里消失——早期漏算 REVIEWED，导致课程统计偏低。
     */
    private static boolean isSubmitted(String status) {
        return "SUBMITTED".equals(status) || "GRADED".equals(status) || "REVIEWED".equals(status);
    }

    /** 已批改：AI 已评待确认 / 教师已终审；终审成绩是最权威成绩，理应计入平均分 */
    private static boolean isGraded(String status) {
        return "GRADED".equals(status) || "REVIEWED".equals(status);
    }

    @Override
    public List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId) {
        Map<Long, SubmissionStatsVO.StudentScoreVO> map = new HashMap<>();
        if (courseId == null) {
            return new ArrayList<>(map.values());
        }
        List<AssignmentEntity> assignments = assignmentDao.listByCourseId(courseId);
        List<Long> assignmentIds = assignments.stream().map(AssignmentEntity::getId).collect(Collectors.toList());

        // 真实平均分：累加总分与样本数后一次性相除。
        // 旧实现用 (prev + ratio) / 2 递推近似，答卷数一多会收敛到"最近两次的平均"，
        // 与课程整体均分口径不一致。
        Map<Long, double[]> scoreAgg = new HashMap<>();
        for (SubmissionEntity sub : submissionDao.listByScope(assignmentIds, null, null, null, null, null)) {
            SubmissionStatsVO.StudentScoreVO vo = map.computeIfAbsent(sub.getStudentId(), id -> {
                SubmissionStatsVO.StudentScoreVO s = new SubmissionStatsVO.StudentScoreVO();
                s.setStudentId(id);
                s.setSubmissionCount(0);
                s.setAvgScore(0.0);
                return s;
            });
            vo.setSubmissionCount(vo.getSubmissionCount() + 1);
            if (hasUsableScore(sub)) {
                double[] cell = scoreAgg.computeIfAbsent(sub.getStudentId(), k -> new double[2]);
                cell[0] += sub.getTotalScore() * 100.0 / sub.getMaxScore();
                cell[1] += 1;
            }
        }
        for (Map.Entry<Long, double[]> e : scoreAgg.entrySet()) {
            SubmissionStatsVO.StudentScoreVO vo = map.get(e.getKey());
            if (vo != null && e.getValue()[1] > 0) {
                vo.setAvgScore(Math.round(e.getValue()[0] / e.getValue()[1] * 10.0) / 10.0);
            }
        }
        return new ArrayList<>(map.values());
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

    /** 答卷是否具备可纳入均分计算的分数（已批改且满分有效） */
    private static boolean hasUsableScore(SubmissionEntity sub) {
        return isGraded(sub.getStatus())
                && sub.getTotalScore() != null
                && sub.getMaxScore() != null
                && sub.getMaxScore() > 0;
    }
}
