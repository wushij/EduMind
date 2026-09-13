package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageDao {

    private final MessageMapper messageMapper;

    public List<MessageEntity> listByConversationId(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }
        return messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntity::getCreateTime)
        );
    }

    public int insert(MessageEntity entity) {
        return messageMapper.insert(entity);
    }

    public int deleteById(String id) {
        if (id == null) {
            return 0;
        }
        return messageMapper.deleteById(id);
    }

    public MessageEntity findLastByConversationIdAndRole(String conversationId, String role) {
        if (conversationId == null || role == null) {
            return null;
        }
        return messageMapper.selectOne(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getRole, role)
                        .orderByDesc(MessageEntity::getCreateTime)
                        .last("LIMIT 1")
        );
    }
}
