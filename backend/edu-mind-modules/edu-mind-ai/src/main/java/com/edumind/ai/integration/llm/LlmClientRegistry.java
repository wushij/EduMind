package com.edumind.ai.integration.llm;

import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiModelConfigEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LlmClientRegistry {

    private final LlmProperties llmProperties;
    private final AiModelConfigDao aiModelConfigDao;
    private final ConcurrentHashMap<String, LlmClient> cache = new ConcurrentHashMap<>();

    public LlmClient get(String modelKey) {
        String key = StringUtils.hasText(modelKey) ? modelKey : "mock";
        return cache.computeIfAbsent(key, this::createClient);
    }

    private LlmClient createClient(String modelKey) {
        if ("mock".equalsIgnoreCase(modelKey)) {
            return new MockLlmClient(llmProperties, modelKey);
        }
        AiModelConfigEntity config = aiModelConfigDao.findByModelKey(modelKey);
        LlmProperties props = buildProperties(modelKey, config);
        if (Boolean.TRUE.equals(llmProperties.getMockEnabled()) || !StringUtils.hasText(props.getApiKey())) {
            return new MockLlmClient(llmProperties, modelKey);
        }
        return new OpenAiCompatibleLlmClient(props);
    }

    private LlmProperties buildProperties(String modelKey, AiModelConfigEntity config) {
        LlmProperties props = new LlmProperties();
        props.setProvider(config != null ? config.getProvider() : llmProperties.getProvider());
        props.setApiKey(llmProperties.getApiKey());
        props.setBaseUrl(llmProperties.getBaseUrl());
        props.setModel(modelKey);
        props.setTimeoutMs(llmProperties.getTimeoutMs());
        props.setMockEnabled(llmProperties.getMockEnabled());
        props.setStreamEnabled(llmProperties.getStreamEnabled());
        if ("qwen-turbo".equals(modelKey)) {
            props.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        }
        return props;
    }
}
