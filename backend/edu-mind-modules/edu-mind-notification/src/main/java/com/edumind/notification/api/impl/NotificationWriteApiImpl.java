package com.edumind.notification.api.impl;

import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.notification.service.notification.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationWriteApiImpl implements NotificationWriteApi {

    private final NotificationDispatchService notificationDispatchService;

    @Override
    public void sendToUser(Long userId, String title, String content, String type) {
        notificationDispatchService.sendToUser(userId, title, content, type);
    }

    @Override
    public void sendToUser(Long userId, String title, String content, String type, Long refId) {
        notificationDispatchService.sendToUser(userId, title, content, type, refId);
    }

    @Override
    public void sendToUsers(Long tenantId, List<Long> userIds, String title, String content, String type, Long refId) {
        notificationDispatchService.sendToUsers(tenantId, userIds, title, content, type, refId);
    }
}
