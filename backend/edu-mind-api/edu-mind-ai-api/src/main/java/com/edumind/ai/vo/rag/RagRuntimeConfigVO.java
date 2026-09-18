package com.edumind.ai.vo.rag;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagRuntimeConfigVO {

    private boolean hybridEnabled;
    private double minRrfScore;
    private String retrievalModelDescription;
    private String embeddingModelName;
    private int embeddingDimensions;
}
