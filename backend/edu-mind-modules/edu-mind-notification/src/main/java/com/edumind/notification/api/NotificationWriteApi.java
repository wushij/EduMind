package com.edumind.notification.api;

import java.util.List;

/**
 * 跨模块通知写入 API
 */
public interface NotificationWriteApi {

    void sendToUser(Long userId, String title, String content, String type);

    void sendToUser(Long userId, String title, String content, String type, Long refId);

    void sendToUsers(Long tenantId, List<Long> userIds, String title, String content, String type, Long refId);
}
