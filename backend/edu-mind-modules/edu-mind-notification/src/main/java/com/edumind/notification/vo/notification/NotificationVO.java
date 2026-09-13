package com.edumind.notification.vo.notification;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知展示对象
 */
@Data
public class NotificationVO implements Serializable {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private Long refId;
    private Integer priority;
    private Integer isRead;
    private LocalDateTime createTime;
}
