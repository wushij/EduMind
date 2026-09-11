package com.edumind.system.service.role.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.cache.PermissionCacheService;
import com.edumind.system.converter.RoleConverter;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.service.role.RoleVoAssembler;
import com.edumind.system.dto.role.RoleCreateDTO;
import com.edumind.system.dto.role.RolePermissionsUpdateDTO;
import com.edumind.system.dto.role.RoleUpdateDTO;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.service.role.RoleService;
import com.edumind.system.vo.role.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final RoleConverter roleConverter;
    private final RoleVoAssembler roleVoAssembler;
    private final PermissionCacheService permissionCacheService;

    @Override
    public List<RoleVO> listRoles() {
        return roleVoAssembler.toVOList(roleDao.findAll());
    }

    @Override
    public Long createRole(RoleCreateDTO dto) {
        if (roleDao.findByRoleCode(dto.getRoleCode()) != null) {
            throw new BusinessException("角色编码已存在");
        }
        RoleEntity entity = roleConverter.toEntity(dto);
        roleDao.insert(entity);
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            permissionDao.replaceRolePermissions(entity.getId(), dto.getPermissionIds());
        }
        return entity.getId();
    }

    @Override
    public RoleVO updateRole(Long id, RoleUpdateDTO dto) {
        RoleEntity entity = roleDao.findById(id);
        if (entity == null) {
            throw new BusinessException("角色不存在");
        }
        roleConverter.applyUpdate(entity, dto);
        roleDao.updateById(entity);
        if (dto.getPermissionIds() != null) {
            permissionDao.replaceRolePermissions(id, dto.getPermissionIds());
            permissionCacheService.evictByRole(id);
        }
        return roleVoAssembler.toVO(entity);
    }

    @Override
    public void deleteRole(Long id) {
        RoleEntity entity = roleDao.findById(id);
        if (entity == null) {
            throw new BusinessException("角色不存在");
        }
        permissionDao.deleteByRoleId(id);
        permissionCacheService.evictByRole(id);
        roleDao.deleteById(id);
    }

    @Override
    public RoleVO updateRolePermissions(Long id, RolePermissionsUpdateDTO dto) {
        RoleEntity entity = roleDao.findById(id);
        if (entity == null) {
            throw new BusinessException("角色不存在");
        }
        permissionDao.replaceRolePermissions(id, dto.getPermissionIds());
        permissionCacheService.evictByRole(id);
        return roleVoAssembler.toVO(entity);
    }
}
