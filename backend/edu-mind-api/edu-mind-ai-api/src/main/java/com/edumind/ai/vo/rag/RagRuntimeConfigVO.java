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
    /** true=当前向量来自 Mock（未接入真实 Embedding Key），调用方需据此提示用户 */
    private boolean embeddingMocked;
}
