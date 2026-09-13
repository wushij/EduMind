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
        payload.setUnreadCount(notificationDao.countUnreadByUserId(userId));

        Map<String, Object> envelope = new HashMap<>(2);
        envelope.put("type", "notification");
        envelope.put("data", payload);
        hub.pushToUser(userId, JSON.toJSONString(envelope));
    }
}
