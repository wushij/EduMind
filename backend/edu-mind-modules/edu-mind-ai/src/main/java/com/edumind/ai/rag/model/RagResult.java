package com.edumind.ai.rag.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagResult {
    private String originalQuery;
    private String rewrittenQuery;
    private List<RetrievalHit> retrievalResults;
    private String context;
    private String promptPreview;
    private String answer;
}
