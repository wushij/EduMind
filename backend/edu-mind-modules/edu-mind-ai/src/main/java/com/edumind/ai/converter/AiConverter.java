package com.edumind.ai.converter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.vo.AiToolVO;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;
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
}
