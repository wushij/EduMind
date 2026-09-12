package com.edumind.knowledge.vo.graph;

import lombok.Data;

@Data
public class KnowledgePointRelationVO {
    private Long id;
    private Long sourceKnowledgePointId;
    private Long targetKnowledgePointId;
    private String relationType;
}
