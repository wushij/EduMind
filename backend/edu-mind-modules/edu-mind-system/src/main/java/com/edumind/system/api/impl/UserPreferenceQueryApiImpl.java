package com.edumind.system.api.impl;

import com.edumind.system.api.UserPreferenceQueryApi;
import com.edumind.system.dao.UserPreferenceDao;
import com.edumind.system.entity.UserPreferenceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceQueryApiImpl implements UserPreferenceQueryApi {

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
}
