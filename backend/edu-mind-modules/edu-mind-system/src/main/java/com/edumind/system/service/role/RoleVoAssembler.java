package com.edumind.system.service.role;

import com.edumind.system.converter.RoleConverter;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.vo.role.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleVoAssembler {

    private final PermissionDao permissionDao;
    private final RoleConverter roleConverter;

    public RoleVO toVO(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        List<String> permissions = permissionDao.findByRoleId(entity.getId()).stream()
                .map(PermissionEntity::getPermissionCode)
                .collect(Collectors.toList());
        return roleConverter.toVO(entity, permissions);
    }

    public List<RoleVO> toVOList(List<RoleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
