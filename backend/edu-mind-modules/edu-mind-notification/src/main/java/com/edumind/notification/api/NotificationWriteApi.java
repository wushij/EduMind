package com.edumind.notification.api;

/**
 * 跨模块通知写入 API
 */
public interface NotificationWriteApi {

    void sendToUser(Long userId, String title, String content, String type);

    void sendToUser(Long userId, String title, String content, String type, Long refId);
}
