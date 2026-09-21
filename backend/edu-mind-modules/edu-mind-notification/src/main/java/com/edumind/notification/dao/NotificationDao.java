package com.edumind.notification.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class NotificationDao {

    /** 批量插入单条 SQL 的最大行数，避免超出 MySQL max_allowed_packet */
    private static final int INSERT_BATCH_SIZE = 500;

    private final NotificationMapper notificationMapper;

    public NotificationEntity findById(Long id) {
        return notificationMapper.selectById(id);
    }

    public List<NotificationEntity> findByUserId(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .orderByDesc(NotificationEntity::getCreateTime)
        );
    }

    public List<NotificationEntity> findUnreadByUserId(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .eq(NotificationEntity::getIsRead, 0)
                        .orderByDesc(NotificationEntity::getCreateTime)
        );
    }

    public long countUnreadByUserId(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .eq(NotificationEntity::getIsRead, 0)
        );
    }

    public long countByUserId(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
        );
    }

    public PageResult<NotificationEntity> pageByUserId(Long userId, String category, long pageNum, long pageSize) {
        LambdaQueryWrapper<NotificationEntity> wrapper = new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getUserId, userId)
                .orderByDesc(NotificationEntity::getCreateTime);
        applyCategoryFilter(wrapper, category);
        Page<NotificationEntity> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.<NotificationEntity>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(page.getRecords())
                .build();
    }

    public int markAsRead(Long id, Long userId) {
        return notificationMapper.update(
                null,
                new LambdaUpdateWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getId, id)
                        .eq(NotificationEntity::getUserId, userId)
                        .set(NotificationEntity::getIsRead, 1)
        );
    }

    public int markAllAsReadByUserId(Long userId) {
        return notificationMapper.update(
                null,
                new LambdaUpdateWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .eq(NotificationEntity::getIsRead, 0)
                        .set(NotificationEntity::getIsRead, 1)
        );
    }

    public List<Long> findUnreadBroadcastRefIdsByUserId(Long userId) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        return notificationMapper.selectList(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .eq(NotificationEntity::getType, "BROADCAST")
                        .eq(NotificationEntity::getIsRead, 0)
                        .isNotNull(NotificationEntity::getRefId)
                        .select(NotificationEntity::getRefId)
        ).stream()
                .map(NotificationEntity::getRefId)
                .collect(java.util.stream.Collectors.toList());
    }

    public int deleteById(Long id, Long userId) {
        return notificationMapper.delete(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getId, id)
                        .eq(NotificationEntity::getUserId, userId)
        );
    }

    public int deleteAllByUserId(Long userId) {
        return notificationMapper.delete(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
        );
    }

    public int insert(NotificationEntity entity) {
        return notificationMapper.insert(entity);
    }

    /** 单条 SQL 分片批量插入（参与当前事务），用于替代循环内逐条 insert */
    public int insertBatch(List<NotificationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (int i = 0; i < entities.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, entities.size());
            affected += notificationMapper.insertBatch(entities.subList(i, end));
        }
        return affected;
    }

    /** 批量统计多个用户的未读通知数（单次 GROUP BY 查询），用于替代循环内逐用户 countUnreadByUserId */
    public Map<Long, Long> countUnreadByUserIds(java.util.Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        QueryWrapper<NotificationEntity> wrapper = new QueryWrapper<NotificationEntity>()
                .select("user_id AS userId", "COUNT(*) AS cnt")
                .in("user_id", userIds)
                .eq("is_read", 0)
                .groupBy("user_id");
        List<Map<String, Object>> rows = notificationMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object userId = row.get("userId");
            Object count = row.get("cnt");
            if (userId instanceof Number idValue && count instanceof Number countValue) {
                result.put(idValue.longValue(), countValue.longValue());
            }
        }
        return result;
    }

    public int deleteByRefId(Long refId) {
        return notificationMapper.delete(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getRefId, refId)
        );
    }

    public int deleteAllByType(String type) {
        return notificationMapper.delete(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getType, type)
        );
    }

    public int deleteAllByTypeAndTenantId(String type, Long tenantId) {
        if (!StringUtils.hasText(type) || tenantId == null) {
            return 0;
        }
        return notificationMapper.delete(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getType, type)
                        .eq(NotificationEntity::getTenantId, tenantId)
        );
    }

    public long countByBroadcast(Long broadcastId) {
        if (broadcastId == null) {
            return 0L;
        }
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getType, "BROADCAST")
                        .eq(NotificationEntity::getRefId, broadcastId)
        );
    }

    public long countReadByBroadcast(Long broadcastId) {
        if (broadcastId == null) {
            return 0L;
        }
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getType, "BROADCAST")
                        .eq(NotificationEntity::getRefId, broadcastId)
                        .eq(NotificationEntity::getIsRead, 1)
        );
    }

    public PageResult<NotificationEntity> pageByBroadcast(Long broadcastId, Integer isRead, List<Long> userIds, long pageNum, long pageSize) {
        LambdaQueryWrapper<NotificationEntity> wrapper = new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getType, "BROADCAST")
                .eq(NotificationEntity::getRefId, broadcastId)
                .orderByDesc(NotificationEntity::getIsRead)
                .orderByDesc(NotificationEntity::getCreateTime);
        if (isRead != null) {
            wrapper.eq(NotificationEntity::getIsRead, isRead);
        }
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return PageResult.<NotificationEntity>builder()
                        .total(0L)
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .list(java.util.Collections.emptyList())
                        .build();
            }
            wrapper.in(NotificationEntity::getUserId, userIds);
        }
        Page<NotificationEntity> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.<NotificationEntity>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(page.getRecords())
                .build();
    }

    private void applyCategoryFilter(LambdaQueryWrapper<NotificationEntity> wrapper, String category) {
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category)) {
            return;
        }
        switch (category.toLowerCase()) {
            case "system":
                wrapper.in(NotificationEntity::getType, "SYSTEM", "BROADCAST");
                break;
            case "teaching":
                wrapper.in(NotificationEntity::getType, "COURSE", "ASSIGNMENT", "EXAM");
                break;
            case "knowledge":
                wrapper.eq(NotificationEntity::getType, "KNOWLEDGE_INDEX");
                break;
            case "ai":
                wrapper.eq(NotificationEntity::getType, "AI_TASK");
                break;
            default:
                break;
        }
    }
}
