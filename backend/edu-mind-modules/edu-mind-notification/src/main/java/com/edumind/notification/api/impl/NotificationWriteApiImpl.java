package com.edumind.notification.api.impl;

import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.notification.converter.notification.NotificationConverter;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.push.NotificationPushService;
import com.edumind.notification.vo.notification.NotificationVO;
import com.edumind.system.api.UserPreferenceQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWriteApiImpl implements NotificationWriteApi {

    private final NotificationDao notificationDao;
    private final NotificationPushService notificationPushService;
    private final UserPreferenceQueryApi userPreferenceQueryApi;

    @Override
    public void sendToUser(Long userId, String title, String content, String type) {
        sendToUser(userId, title, content, type, null);
    }

    @Override
    public void sendToUser(Long userId, String title, String content, String type, Long refId) {
        if (userId == null) {
            return;
        }
        if (!userPreferenceQueryApi.isNotificationEnabled(userId)) {
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type != null ? type : "SYSTEM");
        entity.setRefId(refId);
        entity.setIsRead(0);
        notificationDao.insert(entity);

        NotificationVO vo = NotificationConverter.toVO(entity);
        notificationPushService.pushToUser(userId, vo);
    }
}
