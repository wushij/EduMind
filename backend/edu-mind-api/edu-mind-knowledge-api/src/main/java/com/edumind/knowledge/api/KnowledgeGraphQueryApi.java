package com.edumind.knowledge.api;

import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;

import java.util.List;

public interface KnowledgeGraphQueryApi {

    KnowledgeGraphVO buildGraph(Long knowledgeBaseId, Integer depth, List<String> types);
}
