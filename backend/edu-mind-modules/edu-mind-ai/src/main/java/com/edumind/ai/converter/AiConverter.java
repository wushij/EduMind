package com.edumind.ai.converter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.vo.tool.AiToolAdminVO;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.entity.SysAiQuotaEntity;
import com.edumind.ai.vo.AiToolVO;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.quota.SysAiQuotaVO;
import com.edumind.ai.vo.rag.CitationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
public class AiConverter {

    public AiToolVO toToolVO(AiToolEntity entity) {
        AiToolVO vo = new AiToolVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setIsRecommended(entity.getIsRecommended() != null && entity.getIsRecommended() == 1);
        vo.setIsHot(entity.getIsHot() != null && entity.getIsHot() == 1);
        if (vo.getExecutionMode() == null || vo.getExecutionMode().isBlank()) {
            vo.setExecutionMode("ROUTE");
        }
        return vo;
    }

    public AiToolAdminVO toAdminVO(AiToolEntity entity) {
        AiToolAdminVO vo = new AiToolAdminVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setIsRecommended(entity.getIsRecommended() != null && entity.getIsRecommended() == 1);
        vo.setIsHot(entity.getIsHot() != null && entity.getIsHot() == 1);
        if (vo.getExecutionMode() == null || vo.getExecutionMode().isBlank()) {
            vo.setExecutionMode("ROUTE");
        }
        return vo;
    }

    public AiToolEntity toEntity(AiToolSaveDTO dto) {
        AiToolEntity entity = new AiToolEntity();
        applyCommonFields(entity, dto.getName(), dto.getDescription(), dto.getDetailedIntro(),
                dto.getCategory(), dto.getIcon(), dto.getModelId(), dto.getRoute(),
                dto.getExecutionMode(), dto.getTags(), dto.getIsRecommended(), dto.getIsHot(),
                dto.getSortOrder(), dto.getStatus());
        entity.setId(dto.getId());
        entity.setUseCount(0);
        return entity;
    }

    public void applyUpdate(AiToolEntity entity, AiToolUpdateDTO dto) {
        applyCommonFields(entity, dto.getName(), dto.getDescription(), dto.getDetailedIntro(),
                dto.getCategory(), dto.getIcon(), dto.getModelId(), dto.getRoute(),
                dto.getExecutionMode(), dto.getTags(), dto.getIsRecommended(), dto.getIsHot(),
                dto.getSortOrder(), dto.getStatus());
    }

    private void applyCommonFields(AiToolEntity entity, String name, String description, String detailedIntro,
                                   String category, String icon, String modelId, String route,
                                   String executionMode, String tags, Boolean isRecommended, Boolean isHot,
                                   Integer sortOrder, Integer status) {
        entity.setName(name);
        entity.setDescription(description);
        entity.setDetailedIntro(detailedIntro);
        entity.setCategory(category);
        entity.setIcon(icon);
        entity.setModelId(modelId);
        entity.setRoute(route);
        entity.setExecutionMode(StringUtils.hasText(executionMode) ? executionMode : "ROUTE");
        entity.setTags(tags);
        entity.setIsRecommended(Boolean.TRUE.equals(isRecommended) ? 1 : 0);
        entity.setIsHot(Boolean.TRUE.equals(isHot) ? 1 : 0);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        if (status != null) {
            entity.setStatus(status);
        }
    }

    public ConversationVO toConversationVO(ConversationEntity entity) {
        ConversationVO vo = new ConversationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public MessageVO toMessageVO(MessageEntity entity) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(entity, vo);
        if (StringUtils.hasText(entity.getCitationsJson())) {
            vo.setCitations(JSON.parseObject(entity.getCitationsJson(), new TypeReference<List<CitationVO>>() {}));
        } else {
            vo.setCitations(Collections.emptyList());
        }
        return vo;
    }

    public AiCallLogVO toCallLogVO(AiCallLogEntity entity) {
        AiCallLogVO vo = new AiCallLogVO();
        BeanUtils.copyProperties(entity, vo);
        int prompt = entity.getPromptTokens() != null ? entity.getPromptTokens() : 0;
        int completion = entity.getCompletionTokens() != null ? entity.getCompletionTokens() : 0;
        vo.setTotalTokens(prompt + completion);
        return vo;
    }

    public SysAiQuotaVO toQuotaVO(SysAiQuotaEntity entity) {
        SysAiQuotaVO vo = new SysAiQuotaVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
