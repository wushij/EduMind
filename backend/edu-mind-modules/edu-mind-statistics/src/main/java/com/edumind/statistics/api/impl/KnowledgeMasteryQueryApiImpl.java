package com.edumind.statistics.api.impl;

import com.edumind.common.api.analytics.KnowledgeMasteryQueryApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeMasteryQueryApiImpl implements KnowledgeMasteryQueryApi {

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
}
