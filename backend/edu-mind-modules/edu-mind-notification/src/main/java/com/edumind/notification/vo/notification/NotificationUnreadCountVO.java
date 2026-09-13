package com.edumind.notification.vo.notification;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationUnreadCountVO implements Serializable {
    private Long unreadCount;
    private Long totalCount;
}
