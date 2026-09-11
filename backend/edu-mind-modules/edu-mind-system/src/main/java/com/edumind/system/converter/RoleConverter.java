package com.edumind.system.converter;

import com.edumind.system.dto.role.RoleCreateDTO;
import com.edumind.system.dto.role.RoleUpdateDTO;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.vo.role.RoleVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoleConverter {

    public RoleEntity toEntity(RoleCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        entity.setRoleCode(dto.getRoleCode());
        entity.setRoleName(dto.getRoleName());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    public void applyUpdate(RoleEntity entity, RoleUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (StringUtils.hasText(dto.getRoleName())) {
            entity.setRoleName(dto.getRoleName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
    }

    public RoleVO toVO(RoleEntity entity, List<String> permissions) {
        if (entity == null) {
            return null;
        }
        return RoleVO.builder()
                .id(entity.getId())
                .roleCode(entity.getRoleCode())
                .roleName(entity.getRoleName())
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .build();
    }

    public List<RoleVO> toVOList(List<RoleEntity> entities, java.util.function.Function<RoleEntity, List<String>> permissionLoader) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(entity -> toVO(entity, permissionLoader.apply(entity)))
                .collect(Collectors.toList());
    }
}
