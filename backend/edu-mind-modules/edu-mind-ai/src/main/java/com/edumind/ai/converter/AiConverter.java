package com.edumind.ai.converter;

import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.vo.AiToolVO;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class AiConverter {

    public AiToolVO toToolVO(AiToolEntity entity) {
        AiToolVO vo = new AiToolVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setIsRecommended(entity.getIsRecommended() != null && entity.getIsRecommended() == 1);
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
        return vo;
    }
}
