package com.edumind.ai.integration.embedding;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class MockEmbeddingClient implements EmbeddingClient {

    private final EmbeddingProperties properties;

    @Override
    public List<List<Float>> embed(List<String> texts) {
        List<List<Float>> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(hashToVector(text));
        }
        return vectors;
    }

    @Override
    public String getModelName() {
        return properties.getModel() + "-mock";
    }

    @Override
    public int getDimensions() {
        return properties.getDimensions();
    }

    @Override
    public boolean isMock() {
        return true;
    }

    private List<Float> hashToVector(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        List<Float> vector = new ArrayList<>(properties.getDimensions());
        for (int i = 0; i < properties.getDimensions(); i++) {
            int value = bytes.length > 0 ? bytes[i % bytes.length] : i;
            vector.add((value % 100) / 100.0f);
        }
        return vector;
    }
}
