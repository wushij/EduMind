package com.edumind.notification.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

    public int insert(NotificationEntity entity) {
        return notificationMapper.insert(entity);
    }
}
