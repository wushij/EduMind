package com.edumind.notification.vo.notification;

import com.edumind.common.api.PageResult;
import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationListVO implements Serializable {
    private Long unreadCount;
    private Long totalCount;
    private PageResult<NotificationVO> list;
}
