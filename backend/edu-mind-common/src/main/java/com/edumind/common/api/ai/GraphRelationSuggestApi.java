package com.edumind.common.api.ai;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱关系 AI 建议 API（接口在 common，实现在 ai）
 */
public interface GraphRelationSuggestApi {

    List<Map<String, Object>> suggestRelations(Long knowledgeBaseId,
                                               Long sourceKnowledgePointId,
                                               List<Map<String, Object>> candidatePoints,
                                               Integer maxSuggestions);
}
