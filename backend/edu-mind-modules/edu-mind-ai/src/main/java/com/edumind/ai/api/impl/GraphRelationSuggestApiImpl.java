package com.edumind.ai.api.impl;

import com.edumind.ai.service.graph.GraphRelationSuggestService;
import com.edumind.common.api.ai.GraphRelationSuggestApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphRelationSuggestApiImpl implements GraphRelationSuggestApi {

    private final GraphRelationSuggestService graphRelationSuggestService;

    @Override
    public List<Map<String, Object>> suggestRelations(Long knowledgeBaseId,
                                                      Long sourceKnowledgePointId,
                                                      List<Map<String, Object>> candidatePoints,
                                                      Integer maxSuggestions) {
        return graphRelationSuggestService.suggestRelations(
                knowledgeBaseId, sourceKnowledgePointId, candidatePoints, maxSuggestions);
    }
}
