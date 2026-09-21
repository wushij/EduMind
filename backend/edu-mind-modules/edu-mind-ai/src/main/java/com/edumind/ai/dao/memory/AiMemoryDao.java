package com.edumind.ai.dao.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.memory.AiMemoryFeedbackEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.mapper.memory.AiMemoryFeedbackMapper;
import com.edumind.ai.mapper.memory.AiMemoryItemMapper;
import com.edumind.ai.mapper.memory.AiMemoryNamespaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AiMemoryDao {

    private final AiMemoryNamespaceMapper namespaceMapper;
    private final AiMemoryItemMapper itemMapper;
    private final AiMemoryFeedbackMapper feedbackMapper;

    /**
     * 查询用户的记忆命名空间（按课程；courseId 为空表示个人全局空间）。
     *
     * <p>历史数据里同一 (tenant, user, course) 可能存在多条命名空间，
     * 若直接用 {@code selectOne} 会抛 TooManyResultsException 导致整个记忆页面 500。
     * 这里改为取最新一条并记录告警，保证接口可用（重复数据由迁移脚本收敛）。</p>
     */
    public AiMemoryNamespaceEntity findNamespace(Long tenantId, Long userId, Long courseId) {
        LambdaQueryWrapper<AiMemoryNamespaceEntity> wrapper = new LambdaQueryWrapper<AiMemoryNamespaceEntity>()
                .eq(AiMemoryNamespaceEntity::getTenantId, tenantId)
                .eq(AiMemoryNamespaceEntity::getUserId, userId);
        if (courseId != null) {
            wrapper.eq(AiMemoryNamespaceEntity::getCourseId, courseId);
        } else {
            // 全局空间：新写法是占位 ID 0，历史数据可能是 NULL，两者都要能查到
            wrapper.and(w -> w.isNull(AiMemoryNamespaceEntity::getCourseId)
                    .or().eq(AiMemoryNamespaceEntity::getCourseId, AiMemoryNamespaceEntity.GLOBAL_COURSE_ID));
        }
        List<AiMemoryNamespaceEntity> list = namespaceMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        // 优先取占位 0 的规范记录，其次取最新创建的，保证结果稳定
        list.sort((a, b) -> {
            boolean aPlaceholder = isGlobalPlaceholder(a.getCourseId());
            boolean bPlaceholder = isGlobalPlaceholder(b.getCourseId());
            if (aPlaceholder != bPlaceholder) {
                return aPlaceholder ? -1 : 1;
            }
            return Long.compare(b.getId(), a.getId());
        });
        if (list.size() > 1) {
            log.warn("[长期记忆] 检测到重复命名空间 tenant={} user={} course={}，已取 id={}（脚本可收敛重复数据）",
                    tenantId, userId, courseId, list.get(0).getId());
        }
        return list.get(0);
    }

    private static boolean isGlobalPlaceholder(Long courseId) {
        return courseId != null && courseId == AiMemoryNamespaceEntity.GLOBAL_COURSE_ID;
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
