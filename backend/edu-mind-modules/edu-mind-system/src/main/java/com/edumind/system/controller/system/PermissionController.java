package com.edumind.system.controller.system;

import com.edumind.common.api.ApiResult;
import com.edumind.system.service.permission.PermissionService;
import com.edumind.system.vo.permission.PermissionVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @SaCheckPermission("system:role:view")
    @GetMapping
    public ApiResult<List<PermissionVO>> getPermissionTree() {
        return ApiResult.success(permissionService.getPermissionTree());
    }
}
