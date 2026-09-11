package com.edumind.ai.integration.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate = createRestTemplate();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = buildChatBody(systemPrompt, userPrompt, false);
        HttpHeaders headers = buildHeaders();
        JSONObject response = restTemplate.postForObject(
                normalizeBaseUrl() + "/chat/completions",
                new HttpEntity<>(body, headers),
                JSONObject.class
        );
        return extractContent(response);
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        String userPrompt = prompt + "\n请以 JSON 格式返回，包含 questions 数组。";
        return chat("你是专业的教学出题助手，请严格输出 JSON。", userPrompt);
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        try {
            String content = chat(systemPrompt, userPrompt);
            for (char c : content.toCharArray()) {
                callback.onChunk(String.valueOf(c));
            }
            callback.onComplete();
        } catch (Exception ex) {
            callback.onError(ex.getMessage());
        }
    }

    private Map<String, Object> buildChatBody(String systemPrompt, String userPrompt, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("stream", stream);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        return body;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl + "/v1";
        }
        return baseUrl;
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

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        return new RestTemplate(factory);
    }
}
