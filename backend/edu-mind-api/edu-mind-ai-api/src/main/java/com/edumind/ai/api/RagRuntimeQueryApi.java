package com.edumind.ai.api;

import com.edumind.ai.vo.rag.RagRuntimeConfigVO;

public interface RagRuntimeQueryApi {

    RagRuntimeConfigVO getRuntimeConfig();

    long getAverageRecallLatencyMs(Long knowledgeBaseId);
}
