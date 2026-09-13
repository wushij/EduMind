package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;

public interface KnowledgeMasteryService {

    KnowledgeMasteryVO getMastery(Long courseId, Long studentId);

    java.util.Map<String, Object> getHeatmap(Long courseId, String range);

    java.util.Map<String, Object> getHeatmapCell(Long courseId, Long studentId, Long knowledgePointId);

    void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio);
}
