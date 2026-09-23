package com.edumind.ai.api.impl;

import com.edumind.ai.integration.embedding.EmbeddingClient;
import com.edumind.ai.api.embedding.EmbeddingApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingApiImpl implements EmbeddingApi {

    private final EmbeddingClient embeddingClient;

    @Override
    public List<List<Float>> embed(List<String> texts) {
        return embeddingClient.embed(texts);
    }

    @Override
    public String getModelName() {
        return embeddingClient.getModelName();
    }

    @Override
    public int getDimensions() {
        return embeddingClient.getDimensions();
    }

    @Override
    public boolean isMockVector() {
        return embeddingClient.isMock();
    }
}
