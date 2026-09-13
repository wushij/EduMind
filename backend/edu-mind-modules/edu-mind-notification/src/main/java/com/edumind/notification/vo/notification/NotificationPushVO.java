package com.edumind.notification.vo.notification;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebSocket 推送载荷
 */
@Data
public class NotificationPushVO implements Serializable {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long refId;
    private Integer priority;
    private Integer isRead;
    private LocalDateTime createTime;
    private Long unreadCount;
}
