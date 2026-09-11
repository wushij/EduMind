package com.edumind.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.ai.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {
}
