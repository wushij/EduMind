package com.edumind.ai.integration.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.util.BaseUrlNormalizer;
import com.edumind.ai.util.ReasoningEffortNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Slf4j
@RequiredArgsConstructor
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmProperties properties;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, LlmChatOptions.empty());
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, LlmChatOptions options) {
        return chat(systemPrompt, userPrompt, options, null);
    }

    /**
     * 可取消的对话调用。
     *
     * <p>{@code cancelled} 会被 {@code streamFromUpstream} 在 SSE 逐行读取时轮询：
     * 置位后立即退出读取循环并通过 try-with-resources 关闭响应流，
     * 真正断开与上游的连接、停止继续计费（而不是只把结果丢弃）。</p>
     */
    @Override
    public String chat(String systemPrompt, String userPrompt, LlmChatOptions options,
                       BooleanSupplier cancelled) {
        StringBuilder content = new StringBuilder();
        streamChatWithHistory(systemPrompt, List.of(LlmChatMessage.user(userPrompt)), options, cancelled,
                new StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        content.append(chunk);
                    }

                    @Override
                    public void onError(String message) {
                        throw new IllegalStateException(message);
                    }
                });
        // 被取消时上游流会正常收尾到 onComplete，此处必须显式抛出，
        // 否则调用方会拿到半截 JSON 继续解析，白白浪费一次上游额度。
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new LlmCallCancelledException("大模型调用已被用户中止");
        }
        return content.toString();
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        String suffix = "\n请严格只输出一个 JSON 对象，包含 questions 数组（至少 1 题），不要 Markdown 说明或代码围栏外的文字。";
        String marker = "【出题任务目标】";
        int idx = prompt != null ? prompt.indexOf(marker) : -1;
        if (idx > 40) {
            String systemPart = prompt.substring(0, idx).trim();
            String userPart = prompt.substring(idx).trim() + suffix;
            return chat(systemPart, userPart, questionGenerateOptions());
        }
        return chat(
                "你是 EduMind 专业命题助手，请严格按用户要求输出 JSON。",
                (prompt != null ? prompt : "") + suffix,
                questionGenerateOptions()
        );
    }

    private LlmChatOptions questionGenerateOptions() {
        return LlmChatOptions.builder()
                .temperature(0.3)
                .maxTokens(properties.getMaxTokens() != null && properties.getMaxTokens() > 0
                        ? Math.min(properties.getMaxTokens(), 8192)
                        : 8192)
                .build();
    }

    @Override
    public String chatWithHistory(String systemPrompt, List<LlmChatMessage> messages) {
        StringBuilder content = new StringBuilder();
        streamChatWithHistory(systemPrompt, messages, new StreamCallback() {
            @Override
            public void onChunk(String chunk) {
                content.append(chunk);
            }

            @Override
            public void onError(String message) {
                throw new IllegalStateException(message);
            }
        });
        return content.toString();
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        streamChat(systemPrompt, userPrompt, LlmChatOptions.empty(), callback);
    }

    public void streamChat(String systemPrompt, String userPrompt, LlmChatOptions options, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, List.of(LlmChatMessage.user(userPrompt)), options, callback);
    }

    @Override
    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, messages, LlmChatOptions.empty(), callback);
    }

    @Override
    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages,
                                      BooleanSupplier cancelled, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, messages, LlmChatOptions.empty(), cancelled, callback);
    }

    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages,
                                    LlmChatOptions options, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, messages, options, null, callback);
    }

    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages,
                                    LlmChatOptions options, BooleanSupplier cancelled, StreamCallback callback) {
        if (!Boolean.TRUE.equals(properties.getStreamEnabled())) {
            pseudoStream(systemPrompt, messages, options, cancelled, callback);
            return;
        }
        try {
            streamFromUpstream(systemPrompt, messages, options, cancelled, callback);
            callback.onComplete();
        } catch (Exception ex) {
            log.warn("LLM stream failed: {}", ex.getMessage());
            callback.onError(ex.getMessage() != null ? ex.getMessage() : "流式请求失败");
        }
    }

    private void streamFromUpstream(String systemPrompt, List<LlmChatMessage> messages,
                                    LlmChatOptions options, BooleanSupplier cancelled, StreamCallback callback) throws Exception {
        Map<String, Object> body = buildChatBody(systemPrompt, messages, true, options);
        String json = JSON.toJSONString(body);
        String url = normalizeBaseUrl() + "/chat/completions";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        if (StringUtils.hasText(properties.getApiKey())) {
            builder.header("Authorization", "Bearer " + properties.getApiKey());
        }

        HttpResponse<java.io.InputStream> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("上游响应异常 (" + response.statusCode() + "): " + errBody);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith(":")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                parseSseData(data, callback);
            }
        }
    }

    private void parseSseData(String data, StreamCallback callback) {
        JSONObject chunk;
        try {
            chunk = JSON.parseObject(data);
        } catch (Exception ex) {
            return;
        }
        JSONArray choices = chunk.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return;
        }
        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
        if (delta == null) {
            return;
        }
        String reasoning = delta.getString("reasoning_content");
        if (!StringUtils.hasText(reasoning)) {
            reasoning = delta.getString("reasoning");
        }
        if (StringUtils.hasText(reasoning)) {
            callback.onReasoning(reasoning);
        }
        String content = delta.getString("content");
        if (StringUtils.hasText(content)) {
            callback.onChunk(content);
        }
    }

    private void pseudoStream(String systemPrompt, List<LlmChatMessage> messages,
                              LlmChatOptions options, BooleanSupplier cancelled, StreamCallback callback) {
        try {
            Map<String, Object> body = buildChatBody(systemPrompt, messages, false, options);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8));
            if (StringUtils.hasText(properties.getApiKey())) {
                builder.header("Authorization", "Bearer " + properties.getApiKey());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (cancelled != null && cancelled.getAsBoolean()) {
                callback.onComplete();
                return;
            }
            JSONObject json = JSON.parseObject(response.body());
            String content = extractContent(json);
            for (char c : content.toCharArray()) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    break;
                }
                callback.onChunk(String.valueOf(c));
            }
            callback.onComplete();
        } catch (Exception ex) {
            callback.onError(ex.getMessage());
        }
    }

    private Map<String, Object> buildChatBody(String systemPrompt, List<LlmChatMessage> messages,
                                              boolean stream, LlmChatOptions options) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("stream", stream);
        if (stream) {
            body.put("stream_options", Map.of("include_usage", true));
        }
        int maxTokens = options != null && options.getMaxTokens() != null && options.getMaxTokens() > 0
                ? options.getMaxTokens()
                : (properties.getMaxTokens() != null && properties.getMaxTokens() > 0
                ? properties.getMaxTokens() : 8192);
        body.put("max_tokens", maxTokens);
        Double temperature = options != null && options.getTemperature() != null
                ? options.getTemperature()
                : properties.getTemperature();
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        applyThinkingOptions(body, options);
        body.put("messages", buildMessagePayload(systemPrompt, messages));
        return body;
    }

    private List<Map<String, String>> buildMessagePayload(String systemPrompt, List<LlmChatMessage> messages) {
        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""));
        if (messages == null || messages.isEmpty()) {
            payload.add(Map.of("role", "user", "content", ""));
            return payload;
        }
        for (LlmChatMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "assistant" : "user";
            payload.add(Map.of("role", role, "content", message.getContent()));
        }
        return payload;
    }

    /**
     * 对齐 Code Compass：DeepSeek 默认开启 thinking；通义兼容模式开启 enable_thinking。
     * 否则上游不会返回 reasoning_content，前端「深度思考」卡片无内容。
     */
    private void applyThinkingOptions(Map<String, Object> body, LlmChatOptions options) {
        if (options != null && Boolean.TRUE.equals(options.getDisableThinking())) {
            return;
        }
        String provider = properties.getProvider() != null ? properties.getProvider().toLowerCase() : "";
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().toLowerCase() : "";

        if (provider.contains("deepseek") || baseUrl.contains("deepseek")) {
            String effort = ReasoningEffortNormalizer.normalize(properties.getReasoningEffort());
            body.put("reasoning_effort", effort);
            body.put("thinking", Map.of("type", "enabled"));
            return;
        }

        if (provider.contains("qwen") || provider.contains("tongyi") || provider.contains("dashscope")
                || baseUrl.contains("dashscope")) {
            body.put("enable_thinking", true);
        }
    }

    private String normalizeBaseUrl() {
        String normalized = BaseUrlNormalizer.normalize(properties.getBaseUrl());
        return normalized.isEmpty() ? "https://api.deepseek.com/v1" : normalized;
    }

    private String extractContent(JSONObject response) {
        if (response == null) {
            return "";
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message != null ? message.getString("content") : "";
    }
}
