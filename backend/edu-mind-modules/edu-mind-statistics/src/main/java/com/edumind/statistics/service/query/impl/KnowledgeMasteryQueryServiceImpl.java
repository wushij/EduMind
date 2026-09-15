package com.edumind.statistics.service.query.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.service.query.KnowledgeMasteryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeMasteryQueryServiceImpl implements KnowledgeMasteryQueryService {

    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final CourseQueryApi courseQueryApi;

    @Override
    public Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId) {
        List<KnowledgeMasteryEntity> list = knowledgeMasteryDao.listByCourseAndStudent(courseId, studentId);
        Map<Long, Double> map = new HashMap<>();
        for (KnowledgeMasteryEntity entity : list) {
            map.put(entity.getKnowledgePointId(), entity.getMasteryScore().doubleValue());
        }
        return map;
    }

    @Override
    public Map<String, Object> getStudentProfile(Long studentId, Long courseId) {
        Map<Long, Double> mastery = getMasteryByStudentAndCourse(studentId, courseId);
        Map<Long, String> titles = courseQueryApi.listKnowledgePointsByCourseId(courseId).stream()
                .collect(Collectors.toMap(KnowledgePointVO::getId, KnowledgePointVO::getTitle, (a, b) -> a));

        double overall = mastery.isEmpty() ? 0.0
                : mastery.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<String> weak = new ArrayList<>();
        List<String> mastered = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : mastery.entrySet()) {
            String title = titles.getOrDefault(entry.getKey(), "知识点" + entry.getKey());
            if (entry.getValue() < 0.7) {
                weak.add(title);
            } else {
                mastered.add(title);
            }
        }
        weak.sort(Comparator.naturalOrder());

        Map<String, Object> profile = new HashMap<>();
        profile.put("studentId", studentId);
        profile.put("courseId", courseId);
        profile.put("overallMastery", overall);
        profile.put("weakKnowledgePoints", weak);
        profile.put("masteredKnowledgePoints", mastered);
        profile.put("recommendedReviewDurationMinutes", Math.max(15, weak.size() * 10));
        return profile;
    }

    @Override
    public Map<Long, Double> getStudentsAverageMastery(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<KnowledgeMasteryEntity> list = knowledgeMasteryDao.listByStudents(studentIds);
        Map<Long, List<Double>> grouped = new HashMap<>();
        for (KnowledgeMasteryEntity entity : list) {
            if (entity.getMasteryScore() != null) {
                grouped.computeIfAbsent(entity.getStudentId(), k -> new ArrayList<>())
                        .add(entity.getMasteryScore().doubleValue());
            }
        }
        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<Long, List<Double>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.put(entry.getKey(), avg);
        }
        return result;
    }

    @Override
    public Double getClassAverageMastery(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0.0;
        }
        List<KnowledgeMasteryEntity> list = knowledgeMasteryDao.listByStudents(studentIds);
        if (list.isEmpty()) {
            return 0.0;
        }
        return list.stream()
                .filter(e -> e.getMasteryScore() != null)
                .mapToDouble(e -> e.getMasteryScore().doubleValue())
                .average()
                .orElse(0.0);
    }

    @Override
    public Map<String, Object> getStudentOverallProfile(Long studentId) {
        List<KnowledgeMasteryEntity> list = knowledgeMasteryDao.listByStudent(studentId);
        Map<String, Object> profile = new HashMap<>();
        profile.put("studentId", studentId);

        if (list.isEmpty()) {
            profile.put("overallMastery", 0.0);
            profile.put("assessedCount", 0);
            profile.put("weakKnowledgePoints", Collections.emptyList());
            profile.put("masteredKnowledgePoints", Collections.emptyList());
            profile.put("details", Collections.emptyList());
            return profile;
        }

        double overall = list.stream()
                .filter(e -> e.getMasteryScore() != null)
                .mapToDouble(e -> e.getMasteryScore().doubleValue())
                .average()
                .orElse(0.0);

        List<String> weak = new ArrayList<>();
        List<String> mastered = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (KnowledgeMasteryEntity entity : list) {
            double score = entity.getMasteryScore() != null ? entity.getMasteryScore().doubleValue() : 0.0;
            String kpName = "考点/知识点 #" + entity.getKnowledgePointId();
            if (score < 0.7) {
                weak.add(kpName);
            } else {
                mastered.add(kpName);
            }
            Map<String, Object> item = new HashMap<>();
            item.put("knowledgePointId", entity.getKnowledgePointId());
            item.put("name", kpName);
            item.put("score", Math.round(score * 100));
            item.put("sampleCount", entity.getSampleCount());
            item.put("lastAssessedAt", entity.getLastAssessedAt());
            details.add(item);
        }

        profile.put("overallMastery", overall);
        profile.put("assessedCount", list.size());
        profile.put("weakKnowledgePoints", weak);
        profile.put("masteredKnowledgePoints", mastered);
        profile.put("details", details);
        return profile;
    }
}
