package com.edumind.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_notification_broadcast")
public class NotificationBroadcastEntity implements Serializable {
    @TableId(type = IdType.AUTO)
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
