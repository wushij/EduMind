package com.edumind.ai.integration.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DelegatingEmbeddingClient implements EmbeddingClient {

    private final EmbeddingClientRegistry embeddingClientRegistry;

    @Override
    public List<List<Float>> embed(List<String> texts) {
        return embeddingClientRegistry.getClient().embed(texts);
    }

    @Override
    public String getModelName() {
        return embeddingClientRegistry.getClient().getModelName();
    }

    @Override
    public int getDimensions() {
        return embeddingClientRegistry.getClient().getDimensions();
    }

    @Override
    public boolean isMock() {
        return embeddingClientRegistry.getClient().isMock();
    }
}
