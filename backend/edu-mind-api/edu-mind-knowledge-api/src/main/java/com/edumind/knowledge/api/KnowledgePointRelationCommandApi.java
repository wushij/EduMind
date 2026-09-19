package com.edumind.knowledge.api;

import java.util.List;
import java.util.Map;

/**
 * 课程知识点图谱关系（prerequisite 等）跨模块命令与查询。
 */
public interface KnowledgePointRelationCommandApi {

    /**
     * 将 source 知识点的 prerequisite 出边同步为给定目标 ID 列表（全量替换）。
     */
    void syncPrerequisites(Long sourceKnowledgePointId, List<Long> prerequisiteTargetIds);

    /**
     * 删除与指定知识点相关的全部关系边（作为 source 或 target）。
     */
    void deleteAllRelationsForPoint(Long knowledgePointId);

    /**
     * 按知识点 source ID 批量查询 prerequisite 目标 ID（仅查关系表，避免回调 course 模块）。
     * key: sourceKnowledgePointId, value: prerequisite target ids
     */
    Map<Long, List<Long>> listPrerequisiteTargetsBySourceIds(List<Long> sourceKnowledgePointIds);
}
