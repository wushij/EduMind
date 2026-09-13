package com.edumind.notification.service.broadcast;

import com.edumind.notification.converter.notification.NotificationConverter;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.push.NotificationPushService;
import com.edumind.notification.vo.notification.NotificationVO;
import com.edumind.system.api.UserPreferenceQueryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBroadcastDispatcher {

    private final NotificationDao notificationDao;
    private final NotificationPushService notificationPushService;
    private final UserPreferenceQueryApi userPreferenceQueryApi;

    @Async
    public void dispatch(Long broadcastId, List<Long> userIds, String title, String content, int priority) {
        final int batchSize = 200;
        for (int i = 0; i < userIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIds.size());
            List<Long> chunk = userIds.subList(i, end);
            for (Long userId : chunk) {
                if (!userPreferenceQueryApi.isNotificationEnabled(userId)) {
                    continue;
                }
                NotificationEntity entity = new NotificationEntity();
                entity.setUserId(userId);
                entity.setTitle(title);
                entity.setContent(content);
                entity.setType("BROADCAST");
                entity.setRefId(broadcastId);
                entity.setPriority(priority);
                entity.setIsRead(0);
                notificationDao.insert(entity);
                NotificationVO vo = NotificationConverter.toVO(entity);
                notificationPushService.pushToUser(userId, vo);
            }
        }
        log.info("Broadcast dispatched: broadcastId={}, recipients={}", broadcastId, userIds.size());
    }
}
