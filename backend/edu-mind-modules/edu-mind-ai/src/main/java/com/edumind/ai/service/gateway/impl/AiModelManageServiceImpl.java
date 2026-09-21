package com.edumind.ai.service.gateway.impl;

import com.edumind.ai.converter.AiModelConfigConverter;
import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dto.gateway.AiModelSaveDTO;
import com.edumind.ai.dto.gateway.AiModelTestDTO;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.gateway.AiUserModelPolicy;
import com.edumind.ai.integration.crypto.AiApiKeyCipherService;
import com.edumind.ai.integration.llm.AiModelConnectivityTester;
import com.edumind.ai.integration.embedding.EmbeddingClientRegistry;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.service.gateway.AiModelManageService;
import com.edumind.ai.service.gateway.AiProviderPresetCatalog;
import com.edumind.ai.util.ReasoningEffortNormalizer;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.AiModelTestResultVO;
import com.edumind.ai.vo.gateway.AiProviderPresetsResponseVO;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiModelManageServiceImpl implements AiModelManageService {

    private final AiModelConfigDao aiModelConfigDao;
    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiModelConfigConverter aiModelConfigConverter;
    private final AiApiKeyCipherService aiApiKeyCipherService;
    private final AiModelConnectivityTester aiModelConnectivityTester;
    private final AiProviderPresetCatalog aiProviderPresetCatalog;
    private final LlmClientRegistry llmClientRegistry;
    private final EmbeddingClientRegistry embeddingClientRegistry;
    private final AiUserModelPolicy aiUserModelPolicy;

    @Override
    public AiProviderPresetsResponseVO getProviderPresets() {
        return aiProviderPresetCatalog.getPresets();
    }

    @Override
    public List<AiModelConfigVO> listModels(String configType, String status) {
        return aiModelConfigDao.listByFilter(configType, status).stream()
                .map(aiModelConfigConverter::toVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModelConfigVO createModel(AiModelSaveDTO dto) {
        validateSaveDto(dto, true);
        String configName = dto.getName().trim();
        if (aiModelConfigDao.findByConfigName(configName) != null) {
            throw new BusinessException("配置标识已存在: " + configName);
        }

        String configType = normalizeConfigType(dto.getConfigType());
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiModelConfigDao.clearDefaultByType(configType, null);
        }

        AiModelConfigEntity entity = buildEntityFromDto(dto, configName, configType, null);
        if (StringUtils.hasText(dto.getApiKey())) {
            AiApiKeyCipherService.EncryptResult enc = aiApiKeyCipherService.encryptWithVersion(dto.getApiKey());
            if (enc != null) {
                entity.setApiKeyCipher(enc.getCiphertext());
                entity.setKeyVersion(enc.getKeyVersion());
            }
        } else {
            entity.setKeyVersion(1);
        }
        aiModelConfigDao.insert(entity);
        llmClientRegistry.invalidateAll();
        embeddingClientRegistry.invalidate();
        return aiModelConfigConverter.toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(String configName, AiModelSaveDTO dto) {
        AiModelConfigEntity existing = requireByConfigName(configName);
        String configType = StringUtils.hasText(dto.getConfigType())
                ? normalizeConfigType(dto.getConfigType())
                : normalizeConfigType(existing.getConfigType());

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiModelConfigDao.clearDefaultByType(configType, configName);
        }

        applyDto(existing, dto, configType);
        if (StringUtils.hasText(dto.getApiKey())) {
            AiApiKeyCipherService.EncryptResult enc = aiApiKeyCipherService.encryptWithVersion(dto.getApiKey());
            if (enc != null) {
                existing.setApiKeyCipher(enc.getCiphertext());
                existing.setKeyVersion(enc.getKeyVersion());
            }
        }
        aiModelConfigDao.updateById(existing);
        llmClientRegistry.invalidateAll();
        embeddingClientRegistry.invalidate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String configName) {
        requireByConfigName(configName);
        aiModelConfigDao.deleteByConfigName(configName);
        llmClientRegistry.invalidateAll();
        embeddingClientRegistry.invalidate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultModel(String configName) {
        AiModelConfigEntity target = requireByConfigName(configName);
        aiModelConfigDao.clearDefaultByType(normalizeConfigType(target.getConfigType()), configName);
        target.setIsDefault(true);
        target.setEnabled(true);
        aiModelConfigDao.updateById(target);
        syncGatewayRoutesForDefault(target);
        llmClientRegistry.invalidateAll();
        embeddingClientRegistry.invalidate();
    }

    private void syncGatewayRoutesForDefault(AiModelConfigEntity target) {
        if (!"chat".equalsIgnoreCase(normalizeConfigType(target.getConfigType()))) {
            return;
        }
        String lookupKey = StringUtils.hasText(target.getConfigName()) ? target.getConfigName() : target.getModelKey();
        for (String scene : List.of("CHAT", "RAG", "AGENT", "GRADING", "global_assistant")) {
            AiGatewayRouteEntity route = aiGatewayRouteDao.findByScene(scene);
            if (route == null) {
                continue;
            }
            route.setPrimaryModelKey(lookupKey);
            aiGatewayRouteDao.updateById(route);
        }
    }

    @Override
    public AiModelTestResultVO testSavedModel(String configName) {
        AiModelConfigEntity entity = requireByConfigName(configName);
        String apiKey = aiApiKeyCipherService.decrypt(entity.getApiKeyCipher(), entity.getKeyVersion());
        long latency = aiModelConnectivityTester.test(
                entity.getProvider(),
                entity.getConfigType(),
                entity.getModelName(),
                entity.getBaseUrl(),
                apiKey
        );
        return AiModelTestResultVO.ok(latency);
    }

    @Override
    public AiModelTestResultVO testDraftModel(AiModelTestDTO dto) {
        if (!StringUtils.hasText(dto.getModelName())) {
            throw new BusinessException("请填写模型名称");
        }
        if (!StringUtils.hasText(dto.getProvider())) {
            throw new BusinessException("请选择服务商");
        }
        String apiKey = dto.getApiKey();
        if (!StringUtils.hasText(apiKey) && StringUtils.hasText(dto.getName())) {
            AiModelConfigEntity existing = aiModelConfigDao.findByConfigName(dto.getName().trim());
            if (existing != null) {
                apiKey = aiApiKeyCipherService.decrypt(existing.getApiKeyCipher(), existing.getKeyVersion());
            }
        }
        long latency = aiModelConnectivityTester.test(
                dto.getProvider(),
                dto.getConfigType(),
                dto.getModelName(),
                dto.getBaseUrl(),
                apiKey
        );
        return AiModelTestResultVO.ok(latency);
    }

    @Override
    public List<AiModelConfigVO> listEnabledChatModels() {
        return aiModelConfigDao.listByFilter("chat", "enabled").stream()
                .filter(config -> !isMockConfig(config))
                // 只暴露平台允许用户自选的模型（开关 + 白名单），避免用户在下拉里选到平台未开放的模型
                .filter(config -> aiUserModelPolicy.isSelectable(config.getConfigName()))
                .map(aiModelConfigConverter::toVo)
                .collect(Collectors.toList());
    }

    private boolean isMockConfig(AiModelConfigEntity config) {
        if (config == null) {
            return true;
        }
        return "mock".equalsIgnoreCase(config.getProvider())
                || "mock".equalsIgnoreCase(config.getModelKey())
                || "mock".equalsIgnoreCase(config.getConfigName());
    }

    private AiModelConfigEntity requireByConfigName(String configName) {
        AiModelConfigEntity entity = aiModelConfigDao.findByConfigName(configName);
        if (entity == null) {
            throw new BusinessException("模型配置不存在: " + configName);
        }
        return entity;
    }

    private void validateSaveDto(AiModelSaveDTO dto, boolean creating) {
        if (creating && !StringUtils.hasText(dto.getName())) {
            throw new BusinessException("配置名称不能为空");
        }
        if (!StringUtils.hasText(dto.getProvider())) {
            throw new BusinessException("模型服务商不能为空");
        }
        if (!StringUtils.hasText(dto.getModelName())) {
            throw new BusinessException("模型型号不能为空");
        }
        if ("embedding".equalsIgnoreCase(normalizeConfigType(dto.getConfigType()))
                && (dto.getDimension() == null || dto.getDimension() < 128)) {
            throw new BusinessException("请设置有效的向量维度 (128–4096)");
        }
    }

    private AiModelConfigEntity buildEntityFromDto(AiModelSaveDTO dto, String configName, String configType,
                                                   AiModelConfigEntity existing) {
        AiModelConfigEntity entity = existing != null ? existing : new AiModelConfigEntity();
        entity.setConfigName(configName);
        entity.setModelKey(configName);
        entity.setProvider(dto.getProvider().trim().toLowerCase());
        entity.setConfigType(configType);
        entity.setModelName(dto.getModelName().trim());
        entity.setBaseUrl(StringUtils.hasText(dto.getBaseUrl()) ? dto.getBaseUrl().trim() : "");
        entity.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : new BigDecimal("0.70"));
        entity.setEnabled(!"disabled".equalsIgnoreCase(dto.getStatus()));
        entity.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setPriority(entity.getSortOrder() != null ? entity.getSortOrder() : 0);
        entity.setMaxTokens("embedding".equals(configType) ? 512 : 8192);
        if ("chat".equals(configType)) {
            entity.setReasoningEffort(ReasoningEffortNormalizer.normalize(dto.getReasoningEffort()));
            entity.setDimension(0);
        } else {
            entity.setReasoningEffort("low");
            entity.setDimension(dto.getDimension());
        }
        return entity;
    }

    private void applyDto(AiModelConfigEntity entity, AiModelSaveDTO dto, String configType) {
        if (StringUtils.hasText(dto.getProvider())) {
            entity.setProvider(dto.getProvider().trim().toLowerCase());
        }
        if (StringUtils.hasText(dto.getModelName())) {
            entity.setModelName(dto.getModelName().trim());
        }
        if (dto.getBaseUrl() != null) {
            entity.setBaseUrl(dto.getBaseUrl().trim());
        }
        if (dto.getTemperature() != null) {
            entity.setTemperature(dto.getTemperature());
        }
        if (dto.getStatus() != null) {
            entity.setEnabled(!"disabled".equalsIgnoreCase(dto.getStatus()));
        }
        if (dto.getIsDefault() != null) {
            entity.setIsDefault(dto.getIsDefault());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
            entity.setPriority(dto.getSortOrder());
        }
        entity.setConfigType(configType);
        if ("chat".equals(configType)) {
            entity.setReasoningEffort(ReasoningEffortNormalizer.normalize(
                    dto.getReasoningEffort() != null ? dto.getReasoningEffort() : entity.getReasoningEffort()));
        } else if (dto.getDimension() != null && dto.getDimension() > 0) {
            entity.setDimension(dto.getDimension());
        }
    }

    private String normalizeConfigType(String configType) {
        return "embedding".equalsIgnoreCase(configType) ? "embedding" : "chat";
    }
}
