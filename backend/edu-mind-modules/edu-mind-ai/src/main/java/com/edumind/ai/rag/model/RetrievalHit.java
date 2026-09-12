package com.edumind.ai.rag.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RetrievalHit {
    private Long chunkId;
    private double score;
    private Long documentId;
    private String documentName;
    private Integer pageNo;
    private String excerpt;
    private String heading;
    private Integer chunkIndex;
    private Map<String, Object> metadata;
}
