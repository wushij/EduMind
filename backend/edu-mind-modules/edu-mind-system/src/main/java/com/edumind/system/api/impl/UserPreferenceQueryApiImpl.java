package com.edumind.system.api.impl;

import com.edumind.system.api.UserPreferenceQueryApi;
import com.edumind.system.service.query.UserPreferenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPreferenceQueryApiImpl implements UserPreferenceQueryApi {

    private final UserPreferenceQueryService userPreferenceQueryService;

    @Override
    public boolean isNotificationEnabled(Long userId) {
        return userPreferenceQueryService.isNotificationEnabled(userId);
    }

    @Override
    public Set<Long> filterNotificationEnabled(Collection<Long> userIds) {
        return userPreferenceQueryService.filterNotificationEnabled(userIds);
    }
}
