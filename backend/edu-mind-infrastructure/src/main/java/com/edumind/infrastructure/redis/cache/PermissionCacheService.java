package com.edumind.infrastructure.redis.cache;

import com.alibaba.fastjson2.TypeReference;
import com.edumind.common.constant.RedisConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RBAC 权限/角色缓存服务（租户维度）
 * <p>缓存 Key 形如 {@code edumind:tenant:{tenantId}:rbac:perm:{userId}}，
 * 因角色授权已按租户收敛，同一用户在不同租户下缓存内容不同。</p>
 * <p>失效策略：{@link #evictUser(Long)} 必须覆盖该用户所属的全部租户，
 * 否则「超管在 A 租户给跨租户用户改权限」时 B 租户缓存会残留，造成权限延迟生效（越权窗口）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    /** 无租户上下文时的全局兜底 Key（兼容历史数据） */
    private static final String LEGACY_PERM_PREFIX = RedisConstant.PREFIX + "rbac:perm:";
    private static final String LEGACY_ROLE_PREFIX = RedisConstant.PREFIX + "rbac:role:";

    private final RedisService redisService;
    private final UserQueryApi userQueryApi;

    public List<String> getPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContext.getTenantId();
        String key = RedisKeyBuilder.rbacPermissions(tenantId, userId);
        List<String> cached = redisService.getObject(key, STRING_LIST);
        if (cached != null) {
            return cached;
        }
        List<String> permissions = userQueryApi.getPermissionsByUserId(userId);
        // 仅在具备租户上下文时写入租户维度缓存，避免污染全局兜底 Key
        if (tenantId != null && tenantId > 0) {
            redisService.setObject(key, permissions, RedisConstant.RBAC_TTL_SECONDS);
        }
        return permissions;
    }

    public List<String> getRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContext.getTenantId();
        String key = RedisKeyBuilder.rbacRoles(tenantId, userId);
        List<String> cached = redisService.getObject(key, STRING_LIST);
        if (cached != null) {
            return cached;
        }
        List<String> roles = userQueryApi.getRolesByUserId(userId);
        if (tenantId != null && tenantId > 0) {
            redisService.setObject(key, roles, RedisConstant.RBAC_TTL_SECONDS);
        }
        return roles;
    }

    /**
     * 失效指定用户的全部租户维度 RBAC 缓存（并清理历史全局兜底 Key）。
     * 必须跨租户遍历，否则改权限后其它租户会读到最长 30 分钟的旧权限。
     */
    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        Set<Long> tenantIds = new LinkedHashSet<>();
        try {
            tenantIds.addAll(userQueryApi.listTenantIdsByUserId(userId));
        } catch (Exception e) {
            log.warn("查询用户 {} 所属租户失败，将仅失效当前租户缓存: {}", userId, e.getMessage());
        }
        Long current = TenantContext.getTenantId();
        if (current != null && current > 0) {
            tenantIds.add(current);
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            redisService.delete(RedisKeyBuilder.rbacPermissions(tenantId, userId));
            redisService.delete(RedisKeyBuilder.rbacRoles(tenantId, userId));
        }
        // 历史遗留的无租户前缀 Key（老版本写入），一并清理
        redisService.delete(LEGACY_PERM_PREFIX + userId);
        redisService.delete(LEGACY_ROLE_PREFIX + userId);
    }

    public void evictByRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        for (Long userId : userQueryApi.listUserIdsByRoleId(roleId)) {
            evictUser(userId);
        }
    }

    public void evictByTenant(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        redisService.deleteByPattern(RedisConstant.PREFIX + "tenant:" + tenantId + ":rbac:*");
    }

    public void evictAll() {
        redisService.deleteByPattern(LEGACY_PERM_PREFIX + "*");
        redisService.deleteByPattern(LEGACY_ROLE_PREFIX + "*");
        redisService.deleteByPattern(RedisConstant.PREFIX + "tenant:*:rbac:*");
    }
}
