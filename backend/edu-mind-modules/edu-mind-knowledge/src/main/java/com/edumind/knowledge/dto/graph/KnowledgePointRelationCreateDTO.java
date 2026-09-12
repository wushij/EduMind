package com.edumind.knowledge.dto.graph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgePointRelationCreateDTO {
    @NotNull
    private Long targetKnowledgePointId;
    @NotBlank
    private String relationType;
}
