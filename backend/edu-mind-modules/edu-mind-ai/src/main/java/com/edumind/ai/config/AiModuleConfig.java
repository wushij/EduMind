package com.edumind.ai.config;

import com.edumind.ai.integration.llm.LlmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({LlmProperties.class})
public class AiModuleConfig {
}
