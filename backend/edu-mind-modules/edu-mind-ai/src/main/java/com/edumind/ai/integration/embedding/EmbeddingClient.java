package com.edumind.ai.integration.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<List<Float>> embed(List<String> texts);

    String getModelName();

    int getDimensions();

    /**
     * 是否为 Mock（哈希伪向量）实现。
     *
     * <p>为 true 时表示平台没有配置可用的 Embedding Key，向量由本地哈希生成且不具备语义，
     * 调用方（索引任务、检索看板）必须显式提示用户，不能把这种向量当成真实语义向量。</p>
     */
    default boolean isMock() {
        return false;
    }
}
