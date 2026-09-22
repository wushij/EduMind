package com.edumind.notification.service.broadcast;

import com.edumind.common.context.TenantContext;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBroadcastDispatcher {

    private final NotificationDao notificationDao;
    private final NotificationPushService notificationPushService;
    private final UserPreferenceQueryApi userPreferenceQueryApi;

    @Async
    public void dispatch(Long tenantId, Long broadcastId, List<Long> userIds, String title, String content, int priority) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            List<Long> distinctUserIds = userIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (distinctUserIds.isEmpty()) {
                return;
            }

            // 1) 单次批量过滤关闭通知的用户（替代逐用户偏好查询）
            Set<Long> enabledUserIds = userPreferenceQueryApi.filterNotificationEnabled(distinctUserIds);
            if (enabledUserIds.isEmpty()) {
                log.info("Broadcast skipped (all recipients disabled): tenantId={}, broadcastId={}", tenantId, broadcastId);
                return;
            }
            List<Long> recipients = distinctUserIds.stream()
                    .filter(enabledUserIds::contains)
                    .collect(Collectors.toList());

            // 2) 单条 SQL 批量入库（替代 N 次 insert）
            List<NotificationEntity> entities = new ArrayList<>(recipients.size());
            for (Long userId : recipients) {
                NotificationEntity entity = new NotificationEntity();
                entity.setTenantId(tenantId);
                entity.setUserId(userId);
                entity.setTitle(title);
                entity.setContent(content);
                entity.setType("BROADCAST");
                entity.setRefId(broadcastId);
                entity.setPriority(priority);
                entity.setIsRead(0);
                entities.add(entity);
            }
            notificationDao.insertBatch(entities);

            // 3) 单次 GROUP BY 统计未读数并推送（替代逐用户 count 查询；WebSocket 推送本身仍按用户连接下发）
            Map<Long, Long> unreadCountMap = notificationDao.countUnreadByUserIds(recipients);
            for (NotificationEntity entity : entities) {
                NotificationVO vo = NotificationConverter.toVO(entity);
                notificationPushService.pushToUser(
                        entity.getUserId(), vo, unreadCountMap.getOrDefault(entity.getUserId(), 0L));
            }
            log.info("Broadcast dispatched: tenantId={}, broadcastId={}, recipients={}", tenantId, broadcastId, entities.size());
        } finally {
            // 无条件清理：即使 tenantId 为空也必须清理线程上可能残留的上下文，避免线程池复用串租户
            TenantContext.clear();
        }
    }
}
