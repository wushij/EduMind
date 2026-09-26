package com.edumind.ai.gateway;

public interface ModelRouter {

    String resolveModelKey(String scene, String explicitModelKey);

    String resolveFallback(String modelKey);

    /**
     * 解析场景最终使用的模型展示名（底层 model_name，与 AI 调用日志口径一致）。
     *
     * <p>用于向用户展示"这个能力实际跑在哪个模型上"。工具、Agent 等不显式绑定模型的场景
     * 传 {@code null}，即得到平台默认模型，避免继续展示与运行时不符的历史占位值。</p>
     */
    String resolveModelDisplayName(String scene);
}
