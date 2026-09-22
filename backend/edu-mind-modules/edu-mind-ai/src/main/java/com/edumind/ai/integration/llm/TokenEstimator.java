package com.edumind.ai.integration.llm;

import org.springframework.util.StringUtils;

/**
 * Token 估算器。
 *
 * <p>平台接入的模型多为 OpenAI 兼容协议，无法在网关侧拿到服务商返回的精确 usage，
 * 因此统一采用「字符数 / 4」的启发式估算，口径与 AI 调用审计（{@code ai_call_log}）保持一致，
 * 保证诊断面板的 Token 与审计流水同源、可对照。</p>
 */
public final class TokenEstimator {

    private static final int CHARS_PER_TOKEN = 4;

    private TokenEstimator() {
    }

    /** 估算文本 Token 量；空文本返回 0，非空文本至少返回 1（避免短文本被估成 0） */
    public static int estimate(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}
