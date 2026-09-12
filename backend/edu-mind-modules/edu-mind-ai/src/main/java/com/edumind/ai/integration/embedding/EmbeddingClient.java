package com.edumind.ai.integration.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<List<Float>> embed(List<String> texts);

    String getModelName();

    int getDimensions();
}
