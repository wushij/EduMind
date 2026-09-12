package com.edumind.statistics.api.impl;

import com.edumind.common.api.analytics.KnowledgeMasteryQueryApi;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeMasteryQueryApiImpl implements KnowledgeMasteryQueryApi {

    private final KnowledgeMasteryDao knowledgeMasteryDao;

    @Override
    public Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId) {
        List<KnowledgeMasteryEntity> list = knowledgeMasteryDao.listByCourseAndStudent(courseId, studentId);
        Map<Long, Double> map = new HashMap<>();
        for (KnowledgeMasteryEntity entity : list) {
            map.put(entity.getKnowledgePointId(), entity.getMasteryScore().doubleValue());
        }
        return map;
    }
}
