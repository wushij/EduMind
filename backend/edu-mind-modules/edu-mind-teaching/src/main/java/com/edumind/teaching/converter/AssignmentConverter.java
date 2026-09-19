package com.edumind.teaching.converter;

import com.edumind.teaching.dto.assignment.AssignmentSettingsDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.vo.assignment.AssignmentSettingsVO;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AssignmentConverter {

    private final ObjectMapper objectMapper;

    public AssignmentConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssignmentVO toVO(AssignmentEntity entity) {
        if (entity == null) {
            return null;
        }
        AssignmentVO vo = new AssignmentVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setSettings(parseSettings(entity.getSettingsJson()));
        return vo;
    }

    public String toSettingsJson(AssignmentSettingsDTO settings) {
        if (settings == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public AssignmentSettingsVO parseSettings(String json) {
        if (!StringUtils.hasText(json)) {
            return defaultSettings();
        }
        try {
            AssignmentSettingsVO vo = objectMapper.readValue(json, AssignmentSettingsVO.class);
            if (vo.getAiGradingEnabled() == null) {
                vo.setAiGradingEnabled(true);
            }
            if (vo.getAllowLate() == null) {
                vo.setAllowLate(false);
            }
            if (vo.getInstantFeedback() == null) {
                vo.setInstantFeedback(true);
            }
            return vo;
        } catch (JsonProcessingException e) {
            return defaultSettings();
        }
    }

    public AssignmentSettingsDTO toSettingsDto(AssignmentSettingsVO vo) {
        if (vo == null) {
            return null;
        }
        AssignmentSettingsDTO dto = new AssignmentSettingsDTO();
        dto.setAiGradingEnabled(vo.getAiGradingEnabled());
        dto.setAllowLate(vo.getAllowLate());
        dto.setInstantFeedback(vo.getInstantFeedback());
        return dto;
    }

    private AssignmentSettingsVO defaultSettings() {
        AssignmentSettingsVO vo = new AssignmentSettingsVO();
        vo.setAiGradingEnabled(true);
        vo.setAllowLate(false);
        vo.setInstantFeedback(true);
        return vo;
    }
}
