package com.edumind.notification.vo.broadcast;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class NotificationBroadcastVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String targetType;
    private String targetPayload;
    private String notifyType;
    private Integer priority;
    private Long senderId;
    private String senderName;
    private Integer totalCount;
    private Integer readCount;
    private LocalDateTime createTime;
}
