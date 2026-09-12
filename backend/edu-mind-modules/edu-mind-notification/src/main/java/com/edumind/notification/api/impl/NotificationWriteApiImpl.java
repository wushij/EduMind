package com.edumind.notification.api.impl;

import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWriteApiImpl implements NotificationWriteApi {

    private final NotificationDao notificationDao;

    @Override
    public void sendToUser(Long userId, String title, String content, String type) {
        if (userId == null) {
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type != null ? type : "SYSTEM");
        entity.setIsRead(0);
        notificationDao.insert(entity);
    }
}
