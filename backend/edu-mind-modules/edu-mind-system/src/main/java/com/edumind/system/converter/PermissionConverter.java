package com.edumind.system.converter;

import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.vo.permission.PermissionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PermissionConverter {

    public PermissionVO toVO(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        return PermissionVO.builder()
                .id(entity.getId())
                .permissionCode(entity.getPermissionCode())
                .permissionName(entity.getPermissionName())
                .parentId(entity.getParentId())
                .children(new ArrayList<>())
                .build();
    }

    public List<PermissionVO> toTree(List<PermissionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PermissionVO> nodeMap = entities.stream()
                .collect(Collectors.toMap(PermissionEntity::getId, this::toVO, (a, b) -> a));
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            nodeMap.get(parentId).getChildren().add(node);
        }
        return roots;
    }
}
