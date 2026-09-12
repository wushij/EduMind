package com.edumind.ai.gateway;

import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.entity.AiModelConfigEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ModelRouterImpl implements ModelRouter {

    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiModelConfigDao aiModelConfigDao;

    @Override
    public String resolveModelKey(String scene, String explicitModelKey) {
        if (StringUtils.hasText(explicitModelKey)) {
            return explicitModelKey;
        }
        if (StringUtils.hasText(scene)) {
            AiGatewayRouteEntity route = aiGatewayRouteDao.findByScene(scene);
            if (route != null && StringUtils.hasText(route.getPrimaryModelKey())) {
                return route.getPrimaryModelKey();
            }
        }
        return aiModelConfigDao.listEnabled().stream()
                .findFirst()
                .map(AiModelConfigEntity::getModelKey)
                .orElse("mock");
    }

    public String resolveFallback(String modelKey) {
        AiModelConfigEntity config = aiModelConfigDao.findByModelKey(modelKey);
        if (config != null && StringUtils.hasText(config.getFallbackModelKey())) {
            return config.getFallbackModelKey();
        }
        AiGatewayRouteEntity route = aiGatewayRouteDao.findByScene("CHAT");
        return route != null ? route.getFallbackModelKey() : "mock";
    }
}
