package com.edumind.ai.converter.memory;

import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryConverter {

    public MemoryNamespaceVO toNamespaceVO(AiMemoryNamespaceEntity entity, List<AiMemoryItemEntity> items) {
        if (entity == null) {
            return null;
        }
        MemoryNamespaceVO vo = new MemoryNamespaceVO();
        BeanUtils.copyProperties(entity, vo);
        boolean granted = entity.getConsentStatus() != null && entity.getConsentStatus() == 1;
        vo.setConsentStatus(granted);
        vo.setConsentGranted(granted);
        vo.setRetentionDays(entity.getRetentionDays() != null ? entity.getRetentionDays() : 180);
        if (items != null && !items.isEmpty()) {
            vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        } else {
            vo.setItems(Collections.emptyList());
        }
        return vo;
    }

    public MemoryItemVO toItemVO(AiMemoryItemEntity entity) {
        if (entity == null) {
            return null;
        }
        MemoryItemVO vo = new MemoryItemVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setEncrypted(StringUtils.hasText(entity.getContentCiphertext()));
        vo.setMemoryType(StringUtils.hasText(entity.getMemoryType()) ? entity.getMemoryType() : "PREFERENCE");
        vo.setConfidenceScore(0.95);
        vo.setAccessCount(0);
        return vo;
    }

    public AiMemoryItemEntity toItemEntity(MemoryItemCreateDTO dto, Long namespaceId, String ciphertext,
                                           String vectorRef, LocalDateTime expireTime) {
        AiMemoryItemEntity entity = new AiMemoryItemEntity();
        entity.setNamespaceId(namespaceId);
        entity.setMemoryType(StringUtils.hasText(dto.getMemoryType()) ? dto.getMemoryType() : "PREFERENCE");
        entity.setSummary(dto.getSummary());
        entity.setContentCiphertext(ciphertext);
        entity.setSensitivityLevel(StringUtils.hasText(dto.getSensitivityLevel()) ? dto.getSensitivityLevel() : "NORMAL");
        entity.setVectorRef(vectorRef);
        entity.setExpireTime(expireTime);
        return entity;
    }
}
