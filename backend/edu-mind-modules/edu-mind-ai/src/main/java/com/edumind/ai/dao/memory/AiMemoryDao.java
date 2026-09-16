package com.edumind.ai.dao.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.memory.AiMemoryFeedbackEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.mapper.memory.AiMemoryFeedbackMapper;
import com.edumind.ai.mapper.memory.AiMemoryItemMapper;
import com.edumind.ai.mapper.memory.AiMemoryNamespaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiMemoryDao {

    private final AiMemoryNamespaceMapper namespaceMapper;
    private final AiMemoryItemMapper itemMapper;
    private final AiMemoryFeedbackMapper feedbackMapper;

    public AiMemoryNamespaceEntity findNamespace(Long tenantId, Long userId, Long courseId) {
        LambdaQueryWrapper<AiMemoryNamespaceEntity> wrapper = new LambdaQueryWrapper<AiMemoryNamespaceEntity>()
                .eq(AiMemoryNamespaceEntity::getTenantId, tenantId)
                .eq(AiMemoryNamespaceEntity::getUserId, userId);
        if (courseId != null) {
            wrapper.eq(AiMemoryNamespaceEntity::getCourseId, courseId);
        } else {
            wrapper.isNull(AiMemoryNamespaceEntity::getCourseId);
        }
        return namespaceMapper.selectOne(wrapper);
    }

    public AiMemoryNamespaceEntity findNamespaceById(Long id) {
        return namespaceMapper.selectById(id);
    }

    public int insertNamespace(AiMemoryNamespaceEntity entity) {
        return namespaceMapper.insert(entity);
    }

    public int updateNamespace(AiMemoryNamespaceEntity entity) {
        return namespaceMapper.updateById(entity);
    }

    public List<AiMemoryItemEntity> listItemsByNamespace(Long namespaceId) {
        return itemMapper.selectList(new LambdaQueryWrapper<AiMemoryItemEntity>()
                .eq(AiMemoryItemEntity::getNamespaceId, namespaceId)
                .orderByDesc(AiMemoryItemEntity::getCreateTime));
    }

    public AiMemoryItemEntity findItemById(Long id) {
        return itemMapper.selectById(id);
    }

    public int insertItem(AiMemoryItemEntity entity) {
        return itemMapper.insert(entity);
    }

    public int updateItem(AiMemoryItemEntity entity) {
        return itemMapper.updateById(entity);
    }

    public int deleteItemById(Long id) {
        return itemMapper.deleteById(id);
    }

    public int deleteItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return itemMapper.deleteBatchIds(ids);
    }

    public int deleteItemsByNamespaceId(Long namespaceId) {
        return itemMapper.delete(new LambdaQueryWrapper<AiMemoryItemEntity>()
                .eq(AiMemoryItemEntity::getNamespaceId, namespaceId));
    }

    public List<AiMemoryNamespaceEntity> listNamespacesByUser(Long tenantId, Long userId) {
        return namespaceMapper.selectList(new LambdaQueryWrapper<AiMemoryNamespaceEntity>()
                .eq(AiMemoryNamespaceEntity::getTenantId, tenantId)
                .eq(AiMemoryNamespaceEntity::getUserId, userId)
                .orderByAsc(AiMemoryNamespaceEntity::getCourseId));
    }

    public long countItemsByNamespace(Long namespaceId) {
        return itemMapper.selectCount(new LambdaQueryWrapper<AiMemoryItemEntity>()
                .eq(AiMemoryItemEntity::getNamespaceId, namespaceId));
    }

    public int insertFeedback(AiMemoryFeedbackEntity entity) {
        return feedbackMapper.insert(entity);
    }
}
