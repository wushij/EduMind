package com.edumind.ai.converter;

import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiModelConfigConverter {

    public AiModelConfigVO toVo(AiModelConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setId(entity.getId());
        vo.setName(entity.getConfigName());
        vo.setProvider(entity.getProvider());
        vo.setConfigType(StringUtils.hasText(entity.getConfigType()) ? entity.getConfigType() : "chat");
        vo.setModelName(StringUtils.hasText(entity.getModelName()) ? entity.getModelName() : entity.getModelKey());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setHasApiKey(StringUtils.hasText(entity.getApiKeyCipher()));
        vo.setTemperature(entity.getTemperature());
        vo.setReasoningEffort(StringUtils.hasText(entity.getReasoningEffort()) ? entity.getReasoningEffort() : "low");
        vo.setDimension(entity.getDimension());
        vo.setStatus(Boolean.TRUE.equals(entity.getEnabled()) ? "enabled" : "disabled");
        vo.setIsDefault(Boolean.TRUE.equals(entity.getIsDefault()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setModelKey(StringUtils.hasText(entity.getConfigName()) ? entity.getConfigName() : entity.getModelKey());
        vo.setEnabled(entity.getEnabled());
        vo.setPriority(entity.getPriority());
        vo.setFallbackModelKey(entity.getFallbackModelKey());
        vo.setMaxTokens(entity.getMaxTokens());
        return vo;
    }
}
