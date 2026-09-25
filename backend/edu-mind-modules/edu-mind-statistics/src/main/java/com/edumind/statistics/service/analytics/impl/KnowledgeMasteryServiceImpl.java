package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeMasteryServiceImpl implements KnowledgeMasteryService {

    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final CourseQueryApi courseQueryApi;
    private final UserQueryApi userQueryApi;
    private final OrganizationQueryApi organizationQueryApi;
    private final SubmissionQueryApi submissionQueryApi;

    @Override
    public KnowledgeMasteryVO getMastery(Long courseId, Long studentId) {
        KnowledgeMasteryVO vo = new KnowledgeMasteryVO();
        if (courseId == null) {
            return vo;
        }

        // 1. 获取课程考点并补全章节信息
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, String> chapterNameMap = buildChapterNameMap(courseId);

        if (points.isEmpty()) {
            return vo;
        }

        vo.setTotalKnowledgePoints(points.size());
        for (KnowledgePointVO point : points) {
            vo.getDimensions().add(point.getTitle());
        }

        // 2. 获取真实选课学生名单并补齐学生详情
        Set<Long> studentIdSet = new LinkedHashSet<>(courseQueryApi.listStudentUserIdsByCourseId(courseId));
        List<KnowledgeMasteryEntity> realMasteryEntities = knowledgeMasteryDao.listByCourse(courseId);
        for (KnowledgeMasteryEntity entity : realMasteryEntities) {
            if (entity.getStudentId() != null) {
                studentIdSet.add(entity.getStudentId());
            }
        }
        if (studentId != null) {
            studentIdSet.add(studentId);
        }

        List<Long> studentIdList = new ArrayList<>(studentIdSet);
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(studentIdList);
        Map<Long, MemberOrgBriefVO> orgMap = Collections.emptyMap();
        try {
            orgMap = organizationQueryApi.mapPrimaryClassesByUserIds(null, studentIdList);
        } catch (Exception e) {
            log.warn("Failed to retrieve primary classes for students: {}", e.getMessage());
        }

        // 3. 读取作业提交成绩
        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        Map<Long, SubmissionStatsVO.StudentScoreVO> studentScoreMap = submissionStats.getStudentScores() != null
                ? submissionStats.getStudentScores().stream()
                .collect(Collectors.toMap(SubmissionStatsVO.StudentScoreVO::getStudentId, s -> s, (a, b) -> a))
                : Collections.emptyMap();

        // 4. 读取实测掌握度与错题统计
        Map<String, Double> realMasteryMap = new HashMap<>();
        for (KnowledgeMasteryEntity entity : realMasteryEntities) {
            realMasteryMap.put(entity.getStudentId() + "_" + entity.getKnowledgePointId(),
                    entity.getMasteryScore().doubleValue());
        }

        List<WrongQuestionRecordEntity> wrongRecords = wrongQuestionRecordDao.listByCourse(courseId);
        Map<String, Integer> wrongCountMap = new HashMap<>();
        Map<Long, Integer> kpWrongTotalMap = new HashMap<>();
        Map<Long, Set<Long>> kpWrongStudentMap = new HashMap<>();
        for (WrongQuestionRecordEntity wr : wrongRecords) {
            String key = wr.getStudentId() + "_" + wr.getKnowledgePointId();
            int count = wr.getWrongCount() != null ? wr.getWrongCount() : 1;
            wrongCountMap.put(key, count);
            kpWrongTotalMap.put(wr.getKnowledgePointId(), kpWrongTotalMap.getOrDefault(wr.getKnowledgePointId(), 0) + count);
            kpWrongStudentMap.computeIfAbsent(wr.getKnowledgePointId(), k -> new HashSet<>()).add(wr.getStudentId());
        }

        // 5. 矩阵分值推算（支持真实实测与作业推导融合）
        Map<Long, List<Double>> kpAllScores = new HashMap<>();
        Map<Long, List<Double>> studentAllScores = new HashMap<>();

        for (Long sId : studentIdList) {
            SubmissionStatsVO.StudentScoreVO sScore = studentScoreMap.get(sId);
            double baseRatio = (sScore != null && sScore.getAvgScore() != null)
                    ? Math.max(0.3, Math.min(0.98, sScore.getAvgScore() / 100.0))
                    : 0.72; // 先验良性基准

            for (KnowledgePointVO point : points) {
                String key = sId + "_" + point.getId();
                double score;
                if (realMasteryMap.containsKey(key)) {
                    score = realMasteryMap.get(key);
                } else {
                    int wrongs = wrongCountMap.getOrDefault(key, 0);
                    if (wrongs > 0) {
                        score = Math.max(0.2, baseRatio - wrongs * 0.15);
                    } else {
                        // 微调考点认知维度与重要度
                        int importance = point.getImportance() != null ? point.getImportance() : 3;
                        double adjust = (3 - importance) * 0.03;
                        score = Math.max(0.4, Math.min(0.95, baseRatio + adjust));
                    }
                }
                kpAllScores.computeIfAbsent(point.getId(), k -> new ArrayList<>()).add(score);
                studentAllScores.computeIfAbsent(sId, k -> new ArrayList<>()).add(score);
            }
        }

        // 6. 组装班级平均雷达线
        double sumClassAvg = 0.0;
        int countClassAvg = 0;
        int masteredCount = 0;
        int goodCount = 0;
        int warningCount = 0;

        for (KnowledgePointVO point : points) {
            List<Double> scores = kpAllScores.getOrDefault(point.getId(), Collections.emptyList());
            double avg = scores.isEmpty() ? 70.0
                    : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.7) * 100.0;
            int roundedAvg = (int) Math.round(avg);
            vo.getClassAvg().add(roundedAvg);
            sumClassAvg += avg;
            countClassAvg++;

            if (roundedAvg >= 85) {
                masteredCount++;
            } else if (roundedAvg >= 70) {
                goodCount++;
            } else {
                warningCount++;
            }
        }

        vo.setClassAvgMastery(countClassAvg > 0 ? Math.round((sumClassAvg / countClassAvg) * 10.0) / 10.0 : 0.0);
        vo.setMasteredCount(masteredCount);
        vo.setGoodCount(goodCount);
        vo.setWarningCount(warningCount);
        vo.setStudentCount(studentIdList.size());

        // 7. 组装选课学生个体数据
        if (studentId != null) {
            List<Double> personalScores = studentAllScores.get(studentId);
            if (personalScores != null && personalScores.size() == points.size()) {
                for (Double sc : personalScores) {
                    vo.getPersonal().add((int) Math.round(sc * 100.0));
                }
            } else {
                for (KnowledgePointVO point : points) {
                    vo.getPersonal().add(0);
                }
            }
        }

        // 8. 组装学生花名册供下拉联动
        for (Long sId : studentIdList) {
            UserBriefVO user = userMap.get(sId);
            MemberOrgBriefVO orgBrief = orgMap.get(sId);
            List<Double> scList = studentAllScores.getOrDefault(sId, Collections.emptyList());
            double sAvg = scList.isEmpty() ? 0.0 : scList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0;

            KnowledgeMasteryVO.StudentItemVO stuItem = new KnowledgeMasteryVO.StudentItemVO();
            stuItem.setId(sId);
            stuItem.setName(user != null && user.getRealName() != null ? user.getRealName() : "学员 " + sId);
            stuItem.setUsername(user != null ? user.getUsername() : "student_" + sId);
            if (orgBrief != null && orgBrief.getMemberNo() != null && !orgBrief.getMemberNo().isBlank()) {
                stuItem.setStudentNo(orgBrief.getMemberNo());
            } else {
                stuItem.setStudentNo("STU-" + String.format("%04d", sId));
            }
            stuItem.setAvatar(user != null ? user.getAvatar() : null);
            stuItem.setClassName(orgBrief != null && orgBrief.getName() != null ? orgBrief.getName() : "选课班级");
            stuItem.setMasteryAvg(Math.round(sAvg * 10.0) / 10.0);
            vo.getStudents().add(stuItem);
        }
        vo.getStudents().sort((a, b) -> Double.compare(b.getMasteryAvg(), a.getMasteryAvg()));

        // 9. 组装薄弱考点列表
        List<KnowledgeMasteryVO.WeakPointVO> weakPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            KnowledgePointVO point = points.get(i);
            double pointScore;
            if (studentId != null && !vo.getPersonal().isEmpty()) {
                pointScore = vo.getPersonal().get(i) / 100.0;
            } else {
                pointScore = vo.getClassAvg().get(i) / 100.0;
            }

            if (pointScore < 0.70) {
                KnowledgeMasteryVO.WeakPointVO weak = new KnowledgeMasteryVO.WeakPointVO();
                weak.setKnowledgePointId(point.getId());
                weak.setTitle(point.getTitle());
                weak.setChapterName(chapterNameMap.getOrDefault(point.getChapterId(), "核心章节"));
                weak.setMastery(Math.round(pointScore * 1000.0) / 1000.0);
                weak.setWrongCount(kpWrongTotalMap.getOrDefault(point.getId(), 0));
                weak.setAffectedStudentCount(kpWrongStudentMap.containsKey(point.getId())
                        ? kpWrongStudentMap.get(point.getId()).size()
                        : (int) Math.round(studentIdList.size() * (1.0 - pointScore)));

                if (pointScore < 0.50) {
                    weak.setSuggestion("认知盲区严重，建议优先指派前驱概念重温课件并开展 3~5 道变式专项攻坚");
                } else if (pointScore < 0.60) {
                    weak.setSuggestion("公式推演与边界条件易错，建议派发靶向诊断习题集并安排随堂答疑");
                } else {
                    weak.setSuggestion("中等偏弱，建议在下周随堂测验中设置变式考题进行达标核验");
                }
                weakPoints.add(weak);
            }
        }
        weakPoints.sort(Comparator.comparing(KnowledgeMasteryVO.WeakPointVO::getMastery));
        vo.setWeakPoints(weakPoints.stream().limit(8).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio) {
        if (studentId == null || knowledgePointId == null) {
            return;
        }
        KnowledgeMasteryEntity existing = knowledgeMasteryDao.findByStudentAndKp(studentId, knowledgePointId);
        BigDecimal newScore = BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, scoreRatio)))
                .setScale(4, RoundingMode.HALF_UP);
        if (existing == null) {
            KnowledgeMasteryEntity entity = new KnowledgeMasteryEntity();
            entity.setStudentId(studentId);
            entity.setCourseId(courseId);
            entity.setKnowledgePointId(knowledgePointId);
            entity.setMasteryScore(newScore);
            entity.setSampleCount(1);
            entity.setLastAssessedAt(LocalDateTime.now());
            knowledgeMasteryDao.insert(entity);
            return;
        }
        int count = existing.getSampleCount() + 1;
        double weighted = (existing.getMasteryScore().doubleValue() * existing.getSampleCount() + newScore.doubleValue()) / count;
        existing.setMasteryScore(BigDecimal.valueOf(weighted).setScale(4, RoundingMode.HALF_UP));
        existing.setSampleCount(count);
        existing.setLastAssessedAt(LocalDateTime.now());
        knowledgeMasteryDao.updateById(existing);
    }

    @Override
    public Map<String, Object> getHeatmap(Long courseId, String range) {
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", courseId);

        if (courseId == null) {
            result.put("knowledgePoints", Collections.emptyList());
            result.put("students", Collections.emptyList());
            result.put("cells", Collections.emptyList());
            return result;
        }

        // 1. 考点及章节信息
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, String> chapterNameMap = buildChapterNameMap(courseId);

        List<Map<String, Object>> kpList = new ArrayList<>();
        for (KnowledgePointVO p : points) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getTitle() != null ? p.getTitle() : "知识点" + p.getId());
            item.put("chapterId", p.getChapterId());
            item.put("chapterName", chapterNameMap.getOrDefault(p.getChapterId(), "通用章节"));
            kpList.add(item);
        }
        result.put("knowledgePoints", kpList);

        if (points.isEmpty()) {
            result.put("students", Collections.emptyList());
            result.put("cells", Collections.emptyList());
            return result;
        }

        // 2. 真实选课学生名单
        Set<Long> studentIdSet = new LinkedHashSet<>(courseQueryApi.listStudentUserIdsByCourseId(courseId));
        List<KnowledgeMasteryEntity> masteries = knowledgeMasteryDao.listByCourse(courseId);
        for (KnowledgeMasteryEntity entity : masteries) {
            if (entity.getStudentId() != null) {
                studentIdSet.add(entity.getStudentId());
            }
        }

        List<Long> studentIdList = new ArrayList<>(studentIdSet);
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(studentIdList);
        Map<Long, MemberOrgBriefVO> orgMap = Collections.emptyMap();
        try {
            orgMap = organizationQueryApi.mapPrimaryClassesByUserIds(null, studentIdList);
        } catch (Exception e) {
            log.warn("Failed to retrieve primary classes: {}", e.getMessage());
        }

        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        Map<Long, SubmissionStatsVO.StudentScoreVO> studentScoreMap = submissionStats.getStudentScores() != null
                ? submissionStats.getStudentScores().stream()
                .collect(Collectors.toMap(SubmissionStatsVO.StudentScoreVO::getStudentId, s -> s, (a, b) -> a))
                : Collections.emptyMap();

        Map<String, Double> realMasteryMap = new HashMap<>();
        for (KnowledgeMasteryEntity entity : masteries) {
            realMasteryMap.put(entity.getStudentId() + "_" + entity.getKnowledgePointId(),
                    entity.getMasteryScore().doubleValue());
        }

        List<WrongQuestionRecordEntity> wrongRecords = wrongQuestionRecordDao.listByCourse(courseId);
        Map<String, Integer> wrongCountMap = new HashMap<>();
        for (WrongQuestionRecordEntity wr : wrongRecords) {
            wrongCountMap.put(wr.getStudentId() + "_" + wr.getKnowledgePointId(),
                    wr.getWrongCount() != null ? wr.getWrongCount() : 1);
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        List<Map<String, Object>> cells = new ArrayList<>();

        for (Long sid : studentIdList) {
            UserBriefVO user = userMap.get(sid);
            MemberOrgBriefVO orgBrief = orgMap.get(sid);
            SubmissionStatsVO.StudentScoreVO sScore = studentScoreMap.get(sid);

            Map<String, Object> stuObj = new HashMap<>();
            stuObj.put("id", sid);
            stuObj.put("name", user != null && user.getRealName() != null ? user.getRealName() : "学员 " + sid);
            stuObj.put("studentNo", orgBrief != null && orgBrief.getMemberNo() != null ? orgBrief.getMemberNo() : "STU-" + String.format("%04d", sid));
            stuObj.put("avatar", user != null ? user.getAvatar() : null);
            stuObj.put("className", orgBrief != null && orgBrief.getName() != null ? orgBrief.getName() : "选课班级");
            stuObj.put("avgScore", sScore != null && sScore.getAvgScore() != null ? sScore.getAvgScore() : 75.0);
            studentList.add(stuObj);

            double baseRatio = (sScore != null && sScore.getAvgScore() != null)
                    ? Math.max(0.3, Math.min(0.98, sScore.getAvgScore() / 100.0))
                    : 0.72;

            for (KnowledgePointVO p : points) {
                String key = sid + "_" + p.getId();
                double score;
                if (realMasteryMap.containsKey(key)) {
                    score = realMasteryMap.get(key);
                } else {
                    int wrongs = wrongCountMap.getOrDefault(key, 0);
                    if (wrongs > 0) {
                        score = Math.max(0.2, baseRatio - wrongs * 0.15);
                    } else {
                        int importance = p.getImportance() != null ? p.getImportance() : 3;
                        double adjust = (3 - importance) * 0.03;
                        score = Math.max(0.4, Math.min(0.95, baseRatio + adjust));
                    }
                }

                Map<String, Object> cell = new HashMap<>();
                cell.put("studentId", sid);
                cell.put("knowledgePointId", p.getId());
                cell.put("mastery", Math.round(score * 1000.0) / 1000.0);
                cells.add(cell);
            }
        }

        result.put("students", studentList);
        result.put("cells", cells);
        return result;
    }

    @Override
    public Map<String, Object> getHeatmapCell(Long courseId, Long studentId, Long knowledgePointId) {
        Map<String, Object> cell = new HashMap<>();
        cell.put("courseId", courseId);
        cell.put("studentId", studentId);
        cell.put("knowledgePointId", knowledgePointId);

        KnowledgeMasteryEntity mastery = knowledgeMasteryDao.findByStudentAndKp(studentId, knowledgePointId);
        double masteryScore = mastery != null ? mastery.getMasteryScore().doubleValue() : 0.72;
        cell.put("mastery", masteryScore);

        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        points.stream()
                .filter(p -> knowledgePointId.equals(p.getId()))
                .findFirst()
                .ifPresent(p -> {
                    cell.put("knowledgePointTitle", p.getTitle());
                    cell.put("description", p.getDescription());
                });

        WrongQuestionRecordEntity wrong = wrongQuestionRecordDao.findByStudentCourseAndKp(studentId, courseId, knowledgePointId);
        if (wrong != null) {
            cell.put("diagnosis", wrong.getDiagnosis());
            cell.put("wrongCount", wrong.getWrongCount());
            cell.put("recommendedQuestionIds", wrong.getVariantQuestionIds());
        } else if (masteryScore < 0.55) {
            cell.put("diagnosis", "该考点存在较严重的认知断层，建议由浅入深重构核心定义定理，并进行变式训练");
            cell.put("wrongCount", 3);
        } else if (masteryScore < 0.70) {
            cell.put("diagnosis", "解题方法熟练度不足，复杂边界条件与逆向应用易出错，建议进行针对性强化训练");
            cell.put("wrongCount", 1);
        } else {
            cell.put("diagnosis", "基础掌握良好，建议提供拓展综合题挑战更高阶应用");
            cell.put("wrongCount", 0);
        }
        return cell;
    }

    private Map<Long, String> buildChapterNameMap(Long courseId) {
        Map<Long, String> map = new HashMap<>();
        if (courseId == null) {
            return map;
        }
        try {
            List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
            if (chapters != null) {
                flattenChapters(chapters, map);
            }
        } catch (Exception e) {
            log.warn("Failed to load chapters for course {}: {}", courseId, e.getMessage());
        }
        return map;
    }

    private void flattenChapters(List<ChapterTreeVO> chapters, Map<Long, String> map) {
        for (ChapterTreeVO c : chapters) {
            if (c.getId() != null) {
                map.put(c.getId(), c.getTitle());
            }
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                flattenChapters(c.getChildren(), map);
            }
        }
    }
}

