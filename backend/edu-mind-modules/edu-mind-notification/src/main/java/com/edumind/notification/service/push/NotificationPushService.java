package com.edumind.notification.service.push;

import com.alibaba.fastjson2.JSON;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.vo.notification.NotificationPushVO;
import com.edumind.notification.vo.notification.NotificationVO;
import com.edumind.notification.websocket.NotifyWebSocketHub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final NotifyWebSocketHub hub;
    private final NotificationDao notificationDao;

    public void pushToUser(Long userId, NotificationVO notification) {
        pushToUser(userId, notification, null);
    }

    /**
     * 推送通知给指定用户。
     *
     * @param unreadCount 已批量统计好的未读数；为 null 时回退为单条统计（单用户下发场景）。
     *                    批量下发（广播/课程公告）应传入批量统计结果，避免逐用户 count 查询造成 N+1。
     */
    public void pushToUser(Long userId, NotificationVO notification, Long unreadCount) {
        if (userId == null || notification == null) {
            return;
        }
        NotificationPushVO payload = new NotificationPushVO();
        payload.setId(notification.getId());
        payload.setType(notification.getType());
        payload.setTitle(notification.getTitle());
        payload.setContent(notification.getContent());
        payload.setRefId(notification.getRefId());
        payload.setPriority(notification.getPriority() != null ? notification.getPriority() : 0);
        payload.setIsRead(notification.getIsRead());
        payload.setCreateTime(notification.getCreateTime());
        payload.setUnreadCount(unreadCount != null ? unreadCount : notificationDao.countUnreadByUserId(userId));

        Map<String, Object> envelope = new HashMap<>(2);
        envelope.put("type", "notification");
        envelope.put("data", payload);
        hub.pushToUser(userId, JSON.toJSONString(envelope));
    }
}
