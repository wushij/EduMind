package com.edumind.ai.api.embedding;

import java.util.List;

/**
 * 跨模块 Embedding 能力接口（由 edu-mind-ai 实现，knowledge 模块调用）
 */
public interface EmbeddingApi {

    List<List<Float>> embed(List<String> texts);

    String getModelName();

    int getDimensions();

    /**
     * 当前向量是否来自 Mock（哈希伪向量）。
     *
     * <p>未接入真实 Embedding 模型 / 未配置 API Key 时为 true：此时向量没有语义，
     * 检索命中率不可信，knowledge 模块需要据此在界面上显式提示。</p>
     */
    boolean isMockVector();
}
