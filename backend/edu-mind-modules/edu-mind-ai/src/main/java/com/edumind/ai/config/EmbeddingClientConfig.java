package com.edumind.ai.config;

import com.edumind.ai.integration.embedding.EmbeddingClient;
import com.edumind.ai.integration.embedding.EmbeddingProperties;
import com.edumind.ai.integration.embedding.MockEmbeddingClient;
import com.edumind.ai.integration.embedding.OpenAiCompatibleEmbeddingClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingClientConfig {

    @Bean
    public EmbeddingClient embeddingClient(EmbeddingProperties properties) {
        if (properties.isMockEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return new MockEmbeddingClient(properties);
        }
        return new OpenAiCompatibleEmbeddingClient(properties);
    }
}
