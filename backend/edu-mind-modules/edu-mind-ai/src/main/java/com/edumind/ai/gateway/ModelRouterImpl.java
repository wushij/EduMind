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

    /**
     * 模型解析顺序（与「网关路由」页说明保持一致）：
     * <ol>
     *   <li>显式指定的模型键（会话内用户选择、提示词模板/Agent 绑定）——但用户侧选择需先经
     *       {@link AiUserModelPolicy} 白名单校验，未通过时调用方传 null 进来；</li>
     *   <li>场景策略：管理员在「网关路由与模型调度规则」按业务场景配置的首选模型；</li>
     *   <li>平台默认模型（「模型管理」页 is_default）——场景未配置或路由目标不可用时兜底；</li>
     *   <li>任一可用配置，最后才允许 mock。</li>
     * </ol>
     * 注意：场景策略必须排在平台默认之前，否则管理员配置的场景路由永远不会生效。
     */
    @Override
    public String resolveModelKey(String scene, String explicitModelKey) {
        if (StringUtils.hasText(explicitModelKey)) {
            String normalized = normalizeModelKey(explicitModelKey);
            if (normalized != null) {
                return normalized;
            }
            // 显式 key 在「模型管理」里不存在（前端占位串、已删除或改名的模型）时不能原样下发：
            // LlmClientRegistry 会拿它当模型键去查配置，查不到便落到全局 llm 配置，
            // 全局没有 API Key 时抛 IllegalStateException，表现为与用户所选模型毫无关系的 500。
            // 这里退回场景路由 → 平台默认模型，保证请求至少落在真实可用的模型上。
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

        // 平台默认模型兜底（避免 application-*.yml 的 provider=mock 覆盖已接入的真实模型）
        AiModelConfigEntity defaultChat = aiModelConfigDao.findDefaultByType("chat");
        if (defaultChat != null && isInvokable(defaultChat)) {
            return configLookupKey(defaultChat);
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

    /**
     * @return 归一化后的配置查找键；模型管理里不存在对应配置时返回 {@code null}，
     *         由调用方决定回退到场景路由还是平台默认模型
     */
    private String normalizeModelKey(String modelKey) {
        AiModelConfigEntity config = findConfig(modelKey);
        return config != null ? configLookupKey(config) : null;
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
