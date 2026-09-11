package com.edumind.security.permission;

import cn.dev33.satoken.stp.StpInterface;
import com.edumind.infrastructure.redis.cache.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionCacheService permissionCacheService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> permissions = permissionCacheService.getPermissions(userId);
        return permissions.isEmpty() ? Collections.emptyList() : permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> roles = permissionCacheService.getRoles(userId);
        return roles.isEmpty() ? Collections.emptyList() : roles;
    }

    private Long parseUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.parseLong(loginId.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
