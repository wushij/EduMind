package com.edumind.notification.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 消息通知数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class NotificationDao {

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
