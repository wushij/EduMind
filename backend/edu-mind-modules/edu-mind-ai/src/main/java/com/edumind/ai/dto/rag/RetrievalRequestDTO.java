package com.edumind.ai.dto.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Map;

@Data
public class RetrievalRequestDTO {
    private String query;
    private Integer topK = 5;
    @JsonAlias("scoreThreshold")
    private Double minScore = 0.65;
    private Long documentId;
    private Map<String, Object> filters;
}
