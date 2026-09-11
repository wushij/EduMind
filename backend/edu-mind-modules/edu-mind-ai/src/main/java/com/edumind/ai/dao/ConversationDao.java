package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConversationDao {

    private final ConversationMapper conversationMapper;

    public ConversationEntity findById(String id) {
        return conversationMapper.selectById(id);
    }

    public List<ConversationEntity> findByUserId(Long userId, Long courseId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getUserId, userId)
                .eq(courseId != null, ConversationEntity::getCourseId, courseId)
                .orderByDesc(ConversationEntity::getUpdateTime);
        return conversationMapper.selectList(wrapper);
    }

    public int insert(ConversationEntity entity) {
        return conversationMapper.insert(entity);
    }

    public int updateById(ConversationEntity entity) {
        return conversationMapper.updateById(entity);
    }

    public int softDeleteById(String id) {
        return conversationMapper.deleteById(id);
    }

    public long countAll() {
        return conversationMapper.selectCount(null);
    }
}
