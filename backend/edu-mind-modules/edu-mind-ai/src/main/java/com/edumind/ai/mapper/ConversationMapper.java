package com.edumind.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.ai.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
}
