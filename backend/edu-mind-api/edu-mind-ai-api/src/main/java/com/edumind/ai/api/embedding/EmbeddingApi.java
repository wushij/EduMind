package com.edumind.ai.api.embedding;

import java.util.List;

/**
 * 跨模块 Embedding 能力接口（由 edu-mind-ai 实现，knowledge 模块调用）
 */
public interface EmbeddingApi {

    List<List<Float>> embed(List<String> texts);

    String getModelName();

    int getDimensions();
}
