package com.edumind.ai.service.graph;

import java.util.List;
import java.util.Map;

public interface GraphRelationSuggestService {

    List<Map<String, Object>> suggestRelations(Long knowledgeBaseId,
                                               Long sourceKnowledgePointId,
                                               List<Map<String, Object>> candidatePoints,
                                               Integer maxSuggestions);
}
