package com.edumind.notification.service.notification.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.notification.converter.notification.NotificationConverter;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.notification.NotificationDispatchService;
import com.edumind.notification.service.push.NotificationPushService;
import com.edumind.notification.vo.notification.NotificationVO;
import com.edumind.system.api.UserPreferenceQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationDao notificationDao;
    private final NotificationPushService notificationPushService;
    private final UserPreferenceQueryApi userPreferenceQueryApi;

    @Override
    public void sendToUser(Long userId, String title, String content, String type) {
        sendToUser(userId, title, content, type, null);
    }

    @Override
    public void sendToUser(Long userId, String title, String content, String type, Long refId) {
        if (userId == null) {
            return;
        }
        if (!userPreferenceQueryApi.isNotificationEnabled(userId)) {
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type != null ? type : "SYSTEM");
        entity.setRefId(refId);
        entity.setIsRead(0);
        notificationDao.insert(entity);

        NotificationVO vo = NotificationConverter.toVO(entity);
        notificationPushService.pushToUser(userId, vo);
    }

    @Override
    public void sendToUsers(Long tenantId, List<Long> userIds, String title, String content, String type, Long refId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Long finalTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            if (!userPreferenceQueryApi.isNotificationEnabled(userId)) {
                continue;
            }
            NotificationEntity entity = new NotificationEntity();
            entity.setTenantId(finalTenantId);
            entity.setUserId(userId);
            entity.setTitle(title);
            entity.setContent(content);
            entity.setType(type != null ? type : "SYSTEM");
            entity.setRefId(refId);
            entity.setIsRead(0);
            notificationDao.insert(entity);

            NotificationVO vo = NotificationConverter.toVO(entity);
            notificationPushService.pushToUser(userId, vo);
        }
    }
}
