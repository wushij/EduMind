package com.edumind.system.service.query.impl;

import com.edumind.system.dao.PermissionDao;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.entity.UserRoleEntity;
import com.edumind.system.service.query.UserQueryService;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserDao userDao;
    private final UserRoleDao userRoleDao;
    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final SysTenantMemberDao sysTenantMemberDao;

    @Override
    public UserBriefVO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        // 简要信息仅依赖 sys_user 单表，不再为 4 个字段额外查询角色与权限（原实现为 3 次查询）
        return toBrief(userDao.findById(userId));
    }

    @Override
    public Map<Long, UserBriefVO> mapUserBriefsByIds(Collection<Long> userIds) {
        Set<Long> distinctIds = distinctNonNull(userIds);
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userDao.findByIds(distinctIds).stream()
                .map(UserQueryServiceImpl::toBrief)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserBriefVO::getId, vo -> vo, (a, b) -> a));
    }

    @Override
    public Map<Long, List<String>> mapRoleCodesByUserIds(Collection<Long> userIds) {
        Set<Long> distinctIds = distinctNonNull(userIds);
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserRoleEntity> relations = userRoleDao.findByUserIds(distinctIds);
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyMap();
        }
        Set<Long> roleIds = relations.stream()
                .map(UserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> roleCodeById = roleDao.findByIds(new ArrayList<>(roleIds)).stream()
                .filter(role -> role.getId() != null && role.getRoleCode() != null)
                .collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getRoleCode, (a, b) -> a));
        if (roleCodeById.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> result = new HashMap<>();
        for (UserRoleEntity relation : relations) {
            String roleCode = roleCodeById.get(relation.getRoleId());
            if (relation.getUserId() == null || roleCode == null) {
                continue;
            }
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(roleCode);
        }
        return result;
    }

    private static Set<Long> distinctNonNull(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptySet();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** 与 UserConverter#toVO 保持一致的兜底语义：realName 为空回退用户名，avatar 为空回退空串 */
    private static UserBriefVO toBrief(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserBriefVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .realName(StringUtils.hasText(entity.getRealName()) ? entity.getRealName() : entity.getUsername())
                .avatar(StringUtils.hasText(entity.getAvatar()) ? entity.getAvatar() : "")
                .build();
    }

    @Override
    public List<String> getRolesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return roleDao.findRolesByUserId(userId).stream()
                .map(RoleEntity::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return permissionDao.findPermissionsByUserId(userId).stream()
                .map(PermissionEntity::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return userRoleDao.findUserIdsByRoleId(roleId);
    }

    @Override
    public List<Long> listAllActiveUserIds() {
        return userDao.listAllActiveUserIds();
    }

    @Override
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        RoleEntity role = roleDao.findByRoleCode(roleCode.trim().toUpperCase());
        if (role == null) {
            return Collections.emptyList();
        }
        return userRoleDao.findUserIdsByRoleId(role.getId());
    }

    @Override
    public long countActiveUsers() {
        return userDao.countActiveUsers();
    }

    @Override
    public long countUsersByRoleCode(String roleCode) {
        return listUserIdsByRoleCode(roleCode).size();
    }

    @Override
    public List<Long> listActiveUserIdsByTenantId(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return sysTenantMemberDao.listUserIdsByTenantId(tenantId);
    }

    @Override
    public long countActiveUsersByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        return sysTenantMemberDao.countActiveUsersByTenantId(tenantId);
    }

    @Override
    public List<Long> listUserIdsByTenantAndRole(Long tenantId, String roleCode) {
        if (tenantId == null || roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> tenantUserIds = listActiveUserIdsByTenantId(tenantId);
        if (tenantUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        RoleEntity role = roleDao.findByRoleCode(roleCode.trim().toUpperCase());
        if (role == null) {
            return Collections.emptyList();
        }
        List<Long> roleUserIds = userRoleDao.findUserIdsByRoleId(role.getId());
        if (roleUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> roleUserSet = new HashSet<>(roleUserIds);
        return tenantUserIds.stream()
                .filter(roleUserSet::contains)
                .collect(Collectors.toList());
    }

    @Override
    public long countUsersByTenantAndRole(Long tenantId, String roleCode) {
        return listUserIdsByTenantAndRole(tenantId, roleCode).size();
    }

    @Override
    public List<Long> findUserIdsByKeyword(String keyword) {
        return userDao.findUserIdsByKeyword(keyword);
    }
}
