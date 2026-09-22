package com.edumind.ai.util;

import java.util.regex.Pattern;

/**
 * OpenAI 兼容接口 Base URL 归一化工具。
 *
 * <p>各家控制台与中转站给出的地址有两种形态：基址（{@code https://host/v1}）与完整端点
 * （{@code https://host/v1/chat/completions}）。后台「模型配置」的 Base URL 只接受基址，
 * 调用侧会再追加 {@code /chat/completions}（对话）或 {@code /embeddings}（向量）。
 * 若用户把完整端点整段粘贴进来，就会拼接成
 * {@code https://host/v1/chat/completions/v1/chat/completions} 而 404。
 *
 * <p>本工具统一 {@code OpenAiCompatibleLlmClient}、{@code OpenAiCompatibleEmbeddingClient}
 * 与 {@code AiModelConnectivityTester} 三处行为，保证「测试连接通过 = 实际可调用」。
 */
public final class BaseUrlNormalizer {

    /** 已知的 OpenAI 兼容端点后缀，命中即剥离回基址；长后缀在前，避免被短后缀抢先匹配。 */
    private static final String[] ENDPOINT_SUFFIXES = {"/chat/completions", "/embeddings", "/completions"};

    /** 基址已自带版本段时不再追加，兼容 DeepSeek 的 /v1 与智谱的 /api/paas/v4 等。 */
    private static final Pattern VERSION_SEGMENT = Pattern.compile("/v\\d+$", Pattern.CASE_INSENSITIVE);

    private BaseUrlNormalizer() {
    }

    /**
     * 归一化为「到版本段为止」的基址。
     *
     * <pre>
     * https://api.aigateone.chat/v1/chat/completions -> https://api.aigateone.chat/v1
     * https://api.aigateone.chat/                    -> https://api.aigateone.chat/v1
     * https://open.bigmodel.cn/api/paas/v4           -> 保持不变（已带版本段）
     * </pre>
     *
     * @param rawBaseUrl 用户填写的原始地址，允许为空
     * @return 归一化后的基址；入参为空时返回空串，由调用方决定兜底默认值
     */
    public static String normalize(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return "";
        }
        String url = trimTrailingSlash(rawBaseUrl.trim());
        url = stripEndpointSuffix(url);
        if (url.isEmpty()) {
            return "";
        }
        if (VERSION_SEGMENT.matcher(url).find()) {
            return url;
        }
        return url + "/v1";
    }

    private static String stripEndpointSuffix(String url) {
        for (String suffix : ENDPOINT_SUFFIXES) {
            if (url.length() > suffix.length() && url.toLowerCase().endsWith(suffix)) {
                return trimTrailingSlash(url.substring(0, url.length() - suffix.length()));
            }
        }
        return url;
    }

    private static String trimTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
