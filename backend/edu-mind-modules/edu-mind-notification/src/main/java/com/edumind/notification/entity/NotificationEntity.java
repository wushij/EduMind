package com.edumind.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@TableName("sys_notification")
public class NotificationEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private Long refId;
    private Integer priority;
    private Integer isRead;
    private LocalDateTime createTime;
}
