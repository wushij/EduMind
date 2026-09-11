package com.edumind.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.role.RoleCreateDTO;
import com.edumind.system.dto.role.RolePermissionsUpdateDTO;
import com.edumind.system.dto.role.RoleUpdateDTO;
import com.edumind.system.service.role.RoleService;
import com.edumind.system.vo.role.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @SaCheckPermission("system:role:view")
    @GetMapping
    public ApiResult<List<RoleVO>> listRoles() {
        return ApiResult.success(roleService.listRoles());
    }

    @SaCheckPermission("system:role:edit")
    @PostMapping
    public ApiResult<Long> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        return ApiResult.success(roleService.createRole(dto));
    }

    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}")
    public ApiResult<RoleVO> updateRole(@PathVariable("id") Long id,
                                        @RequestBody RoleUpdateDTO dto) {
        return ApiResult.success(roleService.updateRole(id, dto));
    }

    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}/permissions")
    public ApiResult<RoleVO> updateRolePermissions(@PathVariable("id") Long id,
                                                   @RequestBody RolePermissionsUpdateDTO dto) {
        return ApiResult.success(roleService.updateRolePermissions(id, dto));
    }

    @SaCheckPermission("system:role:edit")
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteRole(@PathVariable("id") Long id) {
        roleService.deleteRole(id);
        return ApiResult.success();
    }
}
