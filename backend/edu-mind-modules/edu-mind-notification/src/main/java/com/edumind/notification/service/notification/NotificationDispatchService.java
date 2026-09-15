package com.edumind.notification.service.notification;

import java.util.List;

public interface NotificationDispatchService {

    void sendToUser(Long userId, String title, String content, String type);

    void sendToUser(Long userId, String title, String content, String type, Long refId);

    void sendToUsers(Long tenantId, List<Long> userIds, String title, String content, String type, Long refId);
}
