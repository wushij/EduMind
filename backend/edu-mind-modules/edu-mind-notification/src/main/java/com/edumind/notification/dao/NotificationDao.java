package com.edumind.notification.dao;

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
        return notificationMapper.selectList(null);
    }
}
