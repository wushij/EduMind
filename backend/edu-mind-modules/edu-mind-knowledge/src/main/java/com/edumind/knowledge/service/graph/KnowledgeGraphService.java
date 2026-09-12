package com.edumind.knowledge.service.graph;

import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;

public interface KnowledgeGraphService {

    KnowledgeGraphVO buildGraph(Long knowledgeBaseId);
}
