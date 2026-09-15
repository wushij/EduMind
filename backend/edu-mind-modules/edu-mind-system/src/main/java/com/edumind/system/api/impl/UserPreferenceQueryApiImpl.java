package com.edumind.system.api.impl;

import com.edumind.system.api.UserPreferenceQueryApi;
import com.edumind.system.service.query.UserPreferenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceQueryApiImpl implements UserPreferenceQueryApi {

    private final UserPreferenceQueryService userPreferenceQueryService;

    @Override
    public boolean isNotificationEnabled(Long userId) {
        return userPreferenceQueryService.isNotificationEnabled(userId);
    }
}
