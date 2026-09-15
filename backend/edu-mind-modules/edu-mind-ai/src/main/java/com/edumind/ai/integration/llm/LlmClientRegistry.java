package com.edumind.ai.integration.llm;

import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.integration.crypto.AiApiKeyCipherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LlmClientRegistry {

    private final LlmProperties llmProperties;
    private final AiModelConfigDao aiModelConfigDao;
    private final AiApiKeyCipherService aiApiKeyCipherService;
    private final ConcurrentHashMap<String, LlmClient> cache = new ConcurrentHashMap<>();

    public LlmClient get(String modelKey) {
        String key = StringUtils.hasText(modelKey) ? modelKey : "mock";
        return cache.computeIfAbsent(key, this::createClient);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public void invalidate(String modelKey) {
        if (StringUtils.hasText(modelKey)) {
            cache.remove(modelKey);
        }
    }

    private LlmClient createClient(String modelKey) {
        if ("mock".equalsIgnoreCase(modelKey)) {
            if (!Boolean.TRUE.equals(llmProperties.getMockEnabled())) {
                throw new IllegalStateException("[GA] prod 环境禁止 mock 模型键: " + modelKey);
            }
            return new MockLlmClient(llmProperties, modelKey);
        }
        AiModelConfigEntity config = aiModelConfigDao.findByModelKey(modelKey);
        if (config == null) {
            config = aiModelConfigDao.findByConfigName(modelKey);
        }
        LlmProperties props = buildProperties(modelKey, config);
        if (Boolean.TRUE.equals(llmProperties.getMockEnabled()) && !StringUtils.hasText(props.getApiKey())) {
            return new MockLlmClient(llmProperties, modelKey);
        }
        if (!Boolean.TRUE.equals(llmProperties.getMockEnabled()) && !StringUtils.hasText(props.getApiKey())) {
            throw new IllegalStateException("[GA] 模型 " + modelKey + " 缺少 API Key，禁止静默 Mock 降级");
        }
        return new OpenAiCompatibleLlmClient(props);
    }

    private LlmProperties buildProperties(String modelKey, AiModelConfigEntity config) {
        LlmProperties props = new LlmProperties();
        if (config != null) {
            props.setProvider(config.getProvider());
            props.setModel(StringUtils.hasText(config.getModelName()) ? config.getModelName() : modelKey);
            props.setBaseUrl(StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl() : llmProperties.getBaseUrl());
            props.setApiKey(aiApiKeyCipherService.decrypt(config.getApiKeyCipher(), config.getKeyVersion()));
            if (!StringUtils.hasText(props.getApiKey())) {
                props.setApiKey(llmProperties.getApiKey());
            }
            if (config.getTemperature() != null) {
                props.setTemperature(config.getTemperature().doubleValue());
            }
            props.setReasoningEffort(config.getReasoningEffort());
            props.setMaxTokens(config.getMaxTokens());
        } else {
            props.setProvider(llmProperties.getProvider());
            props.setApiKey(llmProperties.getApiKey());
            props.setBaseUrl(llmProperties.getBaseUrl());
            props.setModel(modelKey);
            props.setTemperature(llmProperties.getTemperature());
            props.setReasoningEffort(llmProperties.getReasoningEffort());
            props.setMaxTokens(llmProperties.getMaxTokens());
        }
        props.setTimeoutMs(llmProperties.getTimeoutMs());
        props.setMockEnabled(llmProperties.getMockEnabled());
        props.setStreamEnabled(llmProperties.getStreamEnabled());
        return props;
    }
}
