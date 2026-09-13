package com.edumind.ai.integration.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiModelConnectivityTester {

    private static final int TIMEOUT_MS = 15000;

    public long test(String provider, String configType, String modelName, String baseUrl, String apiKey) {
        String normalizedProvider = provider != null ? provider.trim().toLowerCase() : "";
        String normalizedModel = modelName != null ? modelName.trim() : "";
        String type = StringUtils.hasText(configType) ? configType.trim() : "chat";

        if ("mock".equals(normalizedProvider) || normalizedModel.toLowerCase().contains("mock")) {
            sleep(88);
            return 88;
        }

        String resolvedBaseUrl = resolveBaseUrl(normalizedProvider, baseUrl);
        if (!StringUtils.hasText(resolvedBaseUrl)) {
            throw new IllegalArgumentException("未配置请求地址 Base URL");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("请先填写 API Key");
        }

        long start = System.currentTimeMillis();
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        if ("embedding".equalsIgnoreCase(type)) {
            Map<String, Object> body = new HashMap<>();
            body.put("model", normalizedModel);
            body.put("input", "ping");
            post(restTemplate, resolvedBaseUrl + "/embeddings", headers, body);
        } else {
            Map<String, Object> body = new HashMap<>();
            body.put("model", normalizedModel);
            body.put("stream", false);
            body.put("max_tokens", 1);
            body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
            post(restTemplate, resolvedBaseUrl + "/chat/completions", headers, body);
        }
        return System.currentTimeMillis() - start;
    }

    private void post(RestTemplate restTemplate, String url, HttpHeaders headers, Map<String, Object> body) {
        try {
            JSONObject response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), JSONObject.class);
            if (response == null) {
                throw new IllegalStateException("端点响应为空");
            }
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "网络连接失败";
            if (message.contains("401")) {
                throw new IllegalStateException("认证失败 (401): API Key 无效或未授权");
            }
            if (message.contains("404")) {
                throw new IllegalStateException("端点未找到 (404): 请求地址或模型名不存在");
            }
            throw new IllegalStateException("端点响应异常: " + message);
        }
    }

    private String resolveBaseUrl(String provider, String baseUrl) {
        if (StringUtils.hasText(baseUrl)) {
            String trimmed = baseUrl.trim();
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
        return switch (provider) {
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "openai" -> "https://api.openai.com/v1";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "zhipu" -> "https://open.bigmodel.cn/api/paas/v4";
            case "bge" -> "http://127.0.0.1:8080/v1";
            default -> "";
        };
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
