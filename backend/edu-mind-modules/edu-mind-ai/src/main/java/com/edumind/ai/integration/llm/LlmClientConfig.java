package com.edumind.ai.integration.llm;

import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.infrastructure.redis.cache.AiQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmClientConfig {

    @Bean
    public LlmClient llmClient(LlmProperties properties,
                               AiCallLogDao aiCallLogDao,
                               AiQuotaService aiQuotaService,
                               @Value("${edumind.ai.daily-quota:500}") long dailyQuota) {
        LlmClient delegate = createDelegate(properties);
        return new LoggingLlmClient(delegate, aiCallLogDao, properties, aiQuotaService, dailyQuota);
    }

    private LlmClient createDelegate(LlmProperties properties) {
        String provider = properties.getProvider() != null ? properties.getProvider().toLowerCase() : "mock";
        if ("deepseek".equals(provider) || "openai-compatible".equals(provider)) {
            if (!StringUtils.hasText(properties.getApiKey())) {
                log.warn("AI provider={} 但未配置 apiKey，降级为 MockLlmClient", provider);
                return new MockLlmClient(properties);
            }
            return new OpenAiCompatibleLlmClient(properties);
        }
        return new MockLlmClient(properties);
    }
}
