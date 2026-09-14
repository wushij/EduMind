package com.edumind.ai.gateway;

import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.integration.llm.LlmProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ModelRouterImpl implements ModelRouter {

    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiModelConfigDao aiModelConfigDao;
    private final LlmProperties llmProperties;

    @Override
    public String resolveModelKey(String scene, String explicitModelKey) {
        if (StringUtils.hasText(explicitModelKey)) {
            return normalizeModelKey(explicitModelKey);
        }

        // 优先使用后台「模型配置」中的默认模型，避免 application-*.yml 的 provider=mock 覆盖已接入的真实模型
        AiModelConfigEntity defaultChat = aiModelConfigDao.findDefaultByType("chat");
        if (defaultChat != null && isInvokable(defaultChat)) {
            return configLookupKey(defaultChat);
        }

        if (StringUtils.hasText(scene)) {
            AiGatewayRouteEntity route = aiGatewayRouteDao.findByScene(scene);
            if (route != null && StringUtils.hasText(route.getPrimaryModelKey())) {
                AiModelConfigEntity routed = findConfig(route.getPrimaryModelKey());
                if (routed != null && isInvokable(routed)) {
                    return configLookupKey(routed);
                }
            }
        }

        return aiModelConfigDao.listEnabled().stream()
                .filter(this::isInvokable)
                .map(this::configLookupKey)
                .findFirst()
                .orElse("mock");
    }

    @Override
    public String resolveFallback(String modelKey) {
        AiModelConfigEntity config = findConfig(modelKey);
        if (config != null && StringUtils.hasText(config.getFallbackModelKey())
                && (!isMockKey(config.getFallbackModelKey()) || Boolean.TRUE.equals(llmProperties.getMockEnabled()))) {
            return config.getFallbackModelKey();
        }
        AiGatewayRouteEntity route = aiGatewayRouteDao.findByScene("CHAT");
        String routeFallback = route != null ? route.getFallbackModelKey() : null;
        if (StringUtils.hasText(routeFallback) && (!isMockKey(routeFallback) || Boolean.TRUE.equals(llmProperties.getMockEnabled()))) {
            return routeFallback;
        }
        if (Boolean.TRUE.equals(llmProperties.getMockEnabled())) {
            return "mock";
        }
        return null;
    }

    private boolean isMockKey(String modelKey) {
        return "mock".equalsIgnoreCase(modelKey);
    }

    private String normalizeModelKey(String modelKey) {
        AiModelConfigEntity config = findConfig(modelKey);
        return config != null ? configLookupKey(config) : modelKey.trim();
    }

    private AiModelConfigEntity findConfig(String modelKey) {
        if (!StringUtils.hasText(modelKey)) {
            return null;
        }
        AiModelConfigEntity byKey = aiModelConfigDao.findByModelKey(modelKey);
        if (byKey != null) {
            return byKey;
        }
        return aiModelConfigDao.findByConfigName(modelKey);
    }

    private String configLookupKey(AiModelConfigEntity config) {
        if (StringUtils.hasText(config.getConfigName())) {
            return config.getConfigName();
        }
        return config.getModelKey();
    }

    private boolean isInvokable(AiModelConfigEntity config) {
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return false;
        }
        if ("mock".equalsIgnoreCase(config.getProvider()) || "mock".equalsIgnoreCase(config.getModelKey())) {
            return false;
        }
        if (StringUtils.hasText(config.getApiKeyCipher())) {
            return true;
        }
        return StringUtils.hasText(llmProperties.getApiKey());
    }
}
