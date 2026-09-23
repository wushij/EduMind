package com.edumind.ai.api.impl;

import com.edumind.ai.api.RagRuntimeQueryApi;
import com.edumind.ai.api.embedding.EmbeddingApi;
import com.edumind.ai.rag.config.RagProperties;
import com.edumind.ai.vo.rag.RagRuntimeConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagRuntimeQueryApiImpl implements RagRuntimeQueryApi {

    private final RagProperties ragProperties;
    private final EmbeddingApi embeddingApi;
    private final com.edumind.ai.rag.retrieval.VectorRecallLatencyTracker latencyTracker;

    @Override
    public RagRuntimeConfigVO getRuntimeConfig() {
        String retrievalDesc = ragProperties.isHybridEnabled()
                ? "语义向量 + 关键词混合检索"
                : "语义向量检索";
        return RagRuntimeConfigVO.builder()
                .hybridEnabled(ragProperties.isHybridEnabled())
                .minRrfScore(ragProperties.getMinRrfScore())
                .retrievalModelDescription(retrievalDesc)
                .embeddingModelName(embeddingApi.getModelName())
                .embeddingDimensions(embeddingApi.getDimensions())
                .embeddingMocked(embeddingApi.isMockVector())
                .build();
    }

    @Override
    public long getAverageRecallLatencyMs(Long knowledgeBaseId) {
        return latencyTracker != null ? latencyTracker.getAverageLatency(knowledgeBaseId) : 0L;
    }
}
