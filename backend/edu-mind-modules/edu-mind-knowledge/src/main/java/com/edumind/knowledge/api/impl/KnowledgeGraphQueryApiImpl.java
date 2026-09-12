package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.KnowledgeGraphQueryApi;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphQueryApiImpl implements KnowledgeGraphQueryApi {

    private final KnowledgeGraphService knowledgeGraphService;

    @Override
    public KnowledgeGraphVO buildGraph(Long knowledgeBaseId, Integer depth, List<String> types) {
        return knowledgeGraphService.buildGraph(knowledgeBaseId, depth, types);
    }
}
