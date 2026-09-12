package com.edumind.notification.service.notification.impl;

import com.edumind.notification.converter.notification.NotificationConverter;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.notification.NotificationService;
import com.edumind.notification.vo.notification.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    @Override
    public List<NotificationVO> listUnread(Long userId) {
        List<NotificationEntity> entities = notificationDao.findUnreadByUserId(userId);
        return NotificationConverter.toVOList(entities);
    }

    @Override
    public List<NotificationVO> listAll(Long userId) {
        List<NotificationEntity> entities = notificationDao.findByUserId(userId);
        return NotificationConverter.toVOList(entities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id, Long userId) {
        notificationDao.markAsRead(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        notificationDao.markAllAsReadByUserId(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationDao.findUnreadByUserId(userId).size();
    }
}
