package com.edumind.ai.integration.embedding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.util.BaseUrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final EmbeddingProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<List<Float>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<List<Float>> all = new ArrayList<>();
        int batchSize = Math.max(properties.getBatchSize(), 1);
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            all.addAll(requestBatch(batch));
        }
        return all;
    }

    @Override
    public String getModelName() {
        return properties.getModel();
    }

    @Override
    public int getDimensions() {
        return properties.getDimensions();
    }

    private List<List<Float>> requestBatch(List<String> batch) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("input", batch);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        JSONObject response = restTemplate.postForObject(
                normalizeBaseUrl() + "/embeddings",
                new HttpEntity<>(body, headers),
                JSONObject.class
        );
        if (response == null || !response.containsKey("data")) {
            throw new IllegalStateException("Embedding API 响应无效");
        }
        JSONArray data = response.getJSONArray("data");
        List<List<Float>> vectors = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JSONArray embedding = data.getJSONObject(i).getJSONArray("embedding");
            List<Float> vector = new ArrayList<>();
            for (int j = 0; j < embedding.size(); j++) {
                vector.add(embedding.getFloat(j));
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private String normalizeBaseUrl() {
        String normalized = BaseUrlNormalizer.normalize(properties.getBaseUrl());
        return normalized.isEmpty() ? "https://api.openai.com/v1" : normalized;
    }
}
