package com.edumind.knowledge.service.graph;

import com.edumind.knowledge.dto.graph.KnowledgePointRelationCreateDTO;
import com.edumind.knowledge.vo.graph.GraphGapVO;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import com.edumind.knowledge.vo.graph.KnowledgePointRelationVO;

import java.util.List;

public interface KnowledgeGraphService {

    KnowledgeGraphVO buildGraph(Long knowledgeBaseId, Integer depth, List<String> types);

    List<GraphGapVO> findGaps(Long knowledgeBaseId, Long studentId, Double masteryThreshold);

    void createRelation(Long sourceKnowledgePointId, KnowledgePointRelationCreateDTO dto);

    List<KnowledgePointRelationVO> listRelations(Long knowledgePointId);

    void deleteRelation(Long relationId);
}
