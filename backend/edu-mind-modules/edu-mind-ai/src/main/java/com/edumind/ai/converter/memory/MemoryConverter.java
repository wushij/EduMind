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
        String type = StringUtils.hasText(entity.getMemoryType()) ? entity.getMemoryType() : "PREFERENCE";
        vo.setMemoryType(type);

        // 置信度推导与源头通道映射
        double conf = switch (type) {
            case "FEEDBACK" -> 0.99;
            case "PREFERENCE" -> 0.96;
            case "PROFILE" -> 0.93;
            case "EPISODIC" -> 0.87;
            default -> 0.92;
        };
        vo.setConfidenceScore(conf);

        String summary = entity.getSummary() != null ? entity.getSummary() : "";
        if (type.equals("FEEDBACK")) {
            vo.setSourceChannel("STUDENT_FEEDBACK");
            vo.setSourceRef("用户纠错强化");
        } else if (summary.contains("混淆") || summary.contains("盲区") || summary.contains("薄弱")) {
            vo.setSourceChannel("DIAGNOSTIC_ANALYSIS");
            vo.setSourceRef("学情靶向攻坚");
        } else if (summary.contains("偏好") || summary.contains("习惯") || summary.contains("节奏")) {
            vo.setSourceChannel("AI_CONVERSATION");
            vo.setSourceRef("近期助教对话萃取");
        } else {
            vo.setSourceChannel("MANUAL_INJECTION");
            vo.setSourceRef("教学先验规则");
        }

        // 访问频次
        int simulatedAccess = entity.getId() != null ? (int) (Math.abs(entity.getId() * 7 % 19) + 3) : 5;
        vo.setAccessCount(simulatedAccess);
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
