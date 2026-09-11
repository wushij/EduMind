package com.edumind.system.service.role;

import com.edumind.system.dto.role.RoleCreateDTO;
import com.edumind.system.dto.role.RolePermissionsUpdateDTO;
import com.edumind.system.dto.role.RoleUpdateDTO;
import com.edumind.system.vo.role.RoleVO;

import java.util.List;

public interface RoleService {

    List<RoleVO> listRoles();

    Long createRole(RoleCreateDTO dto);

    RoleVO updateRole(Long id, RoleUpdateDTO dto);

    void deleteRole(Long id);

    RoleVO updateRolePermissions(Long id, RolePermissionsUpdateDTO dto);
}
