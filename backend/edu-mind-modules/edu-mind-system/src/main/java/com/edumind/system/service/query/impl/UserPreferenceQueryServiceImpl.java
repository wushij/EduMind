package com.edumind.system.service.query.impl;

import com.edumind.system.dao.UserPreferenceDao;
import com.edumind.system.entity.UserPreferenceEntity;
import com.edumind.system.service.query.UserPreferenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPreferenceQueryServiceImpl implements UserPreferenceQueryService {

    private final UserPreferenceDao userPreferenceDao;

    @Override
    public boolean isNotificationEnabled(Long userId) {
        if (userId == null) {
            return false;
        }
        UserPreferenceEntity entity = userPreferenceDao.findByUserId(userId);
        if (entity == null || entity.getEnableNotification() == null) {
            return true;
        }
        return entity.getEnableNotification() == 1;
    }

    @Override
    public Set<Long> filterNotificationEnabled(Collection<Long> userIds) {
        Set<Long> distinctUserIds = userIds == null
                ? Collections.emptySet()
                : userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinctUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 仅"显式关闭"的用户被剔除；无偏好记录或字段为空视为启用（与单条方法语义一致）
        Set<Long> disabledUserIds = userPreferenceDao.findByUserIds(distinctUserIds).stream()
                .filter(preference -> preference.getEnableNotification() != null
                        && preference.getEnableNotification() != 1)
                .map(UserPreferenceEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (disabledUserIds.isEmpty()) {
            return distinctUserIds;
        }
        return distinctUserIds.stream()
                .filter(userId -> !disabledUserIds.contains(userId))
                .collect(Collectors.toSet());
    }
}
