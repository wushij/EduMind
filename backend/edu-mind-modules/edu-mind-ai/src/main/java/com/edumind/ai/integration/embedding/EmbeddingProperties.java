package com.edumind.ai.integration.embedding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "edumind.embedding")
public class EmbeddingProperties {
    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "text-embedding-3-small";
    private int dimensions = 1536;
    private int batchSize = 16;
    private boolean mockEnabled = true;
}
