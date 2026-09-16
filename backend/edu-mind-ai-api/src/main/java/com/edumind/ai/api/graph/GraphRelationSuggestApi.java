package com.edumind.ai.api.graph;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱关系 AI 建议 API（实现在 ai 模块）
 */
public interface GraphRelationSuggestApi {

    List<Map<String, Object>> suggestRelations(Long knowledgeBaseId,
                                               Long sourceKnowledgePointId,
                                               List<Map<String, Object>> candidatePoints,
                                               Integer maxSuggestions);
}
