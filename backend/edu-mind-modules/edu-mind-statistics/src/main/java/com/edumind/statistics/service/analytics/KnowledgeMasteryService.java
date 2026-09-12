package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;

public interface KnowledgeMasteryService {

    KnowledgeMasteryVO getMastery(Long courseId, Long studentId);

    void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio);
}
