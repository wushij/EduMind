package com.edumind.infrastructure.redis.cache;

import com.alibaba.fastjson2.TypeReference;
import com.edumind.common.constant.RedisConstant;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final RedisService redisService;
    private final UserQueryApi userQueryApi;

    public List<String> getPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String key = RedisKeyBuilder.rbacPermissions(userId);
        List<String> cached = redisService.getObject(key, STRING_LIST);
        if (cached != null) {
            return cached;
        }
        List<String> permissions = userQueryApi.getPermissionsByUserId(userId);
        redisService.setObject(key, permissions, RedisConstant.RBAC_TTL_SECONDS);
        return permissions;
    }

    public List<String> getRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String key = RedisKeyBuilder.rbacRoles(userId);
        List<String> cached = redisService.getObject(key, STRING_LIST);
        if (cached != null) {
            return cached;
        }
        List<String> roles = userQueryApi.getRolesByUserId(userId);
        redisService.setObject(key, roles, RedisConstant.RBAC_TTL_SECONDS);
        return roles;
    }

    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        redisService.delete(RedisKeyBuilder.rbacPermissions(userId));
        redisService.delete(RedisKeyBuilder.rbacRoles(userId));
    }

    public void evictByRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        for (Long userId : userQueryApi.listUserIdsByRoleId(roleId)) {
            evictUser(userId);
        }
    }

    public void evictAll() {
        redisService.deleteByPattern(RedisConstant.RBAC_PERM_KEY + "*");
        redisService.deleteByPattern(RedisConstant.RBAC_ROLE_KEY + "*");
    }
}
