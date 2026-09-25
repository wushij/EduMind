package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;

public interface KnowledgeMasteryService {

    /**
     * 查询课程掌握度画像（默认过滤管理员/测试账号，保持与热力矩阵同一批学员口径）。
     *
     * @param courseId  课程 ID
     * @param studentId 聚焦学员 ID；为 null 时返回全班口径，且不会把调用者身份混入班级统计
     */
    KnowledgeMasteryVO getMastery(Long courseId, Long studentId);

    /**
     * 查询课程掌握度画像。
     *
     * @param includeTesting 是否把管理员/测试账号计入统计。默认 false，
     *                       避免同一页面上「指标条人数」与「热力矩阵人数」互相打架
     */
    KnowledgeMasteryVO getMastery(Long courseId, Long studentId, boolean includeTesting);

    /**
     * 查询班级知识点掌握度热力矩阵（默认过滤管理员/测试账号）。
     */
    java.util.Map<String, Object> getHeatmap(Long courseId, String range);

    /**
     * 查询班级知识点掌握度热力矩阵。
     *
     * @param includeTesting 是否把管理员/测试账号计入统计
     */
    java.util.Map<String, Object> getHeatmap(Long courseId, String range, boolean includeTesting);

    java.util.Map<String, Object> getHeatmapCell(Long courseId, Long studentId, Long knowledgePointId);

    void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio);
}
