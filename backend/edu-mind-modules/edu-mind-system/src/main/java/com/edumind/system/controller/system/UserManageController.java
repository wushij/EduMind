package com.edumind.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.system.dto.user.UserCreateDTO;
import com.edumind.system.dto.user.UserQueryDTO;
import com.edumind.system.dto.user.UserResetPasswordDTO;
import com.edumind.system.dto.user.UserStatusUpdateDTO;
import com.edumind.system.dto.user.UserUpdateDTO;
import com.edumind.system.service.user.UserService;
import com.edumind.system.vo.user.UserVO;
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

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserService userService;

    @SaCheckPermission("system:user:view")
    @GetMapping
    public ApiResult<PageResult<UserVO>> pageUsers(UserQueryDTO query) {
        return ApiResult.success(userService.pageUsers(query));
    }

    @SaCheckPermission("system:user:view")
    @GetMapping("/{id}")
    public ApiResult<UserVO> getUserById(@PathVariable("id") Long id) {
        return ApiResult.success(userService.getUserById(id));
    }

    @SaCheckPermission("system:user:edit")
    @PostMapping
    @com.edumind.common.annotation.OperationLog(module = "用户管理", title = "创建系统用户", businessType = com.edumind.common.enums.BusinessType.INSERT)
    public ApiResult<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(userService.createUser(dto));
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}")
    @com.edumind.common.annotation.OperationLog(module = "用户管理", title = "更新用户信息", businessType = com.edumind.common.enums.BusinessType.UPDATE)
    public ApiResult<UserVO> updateUser(@PathVariable("id") Long id,
                                        @RequestBody UserUpdateDTO dto) {
        return ApiResult.success(userService.updateUser(id, dto));
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/status")
    @com.edumind.common.annotation.OperationLog(module = "用户管理", title = "更新账号状态", businessType = com.edumind.common.enums.BusinessType.GRANT)
    public ApiResult<UserVO> updateUserStatus(@PathVariable("id") Long id,
                                              @Valid @RequestBody UserStatusUpdateDTO dto) {
        return ApiResult.success(userService.updateUserStatus(id, dto));
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/password")
    @com.edumind.common.annotation.OperationLog(module = "用户管理", title = "重置用户登录密码", businessType = com.edumind.common.enums.BusinessType.UPDATE)
    public ApiResult<Void> resetUserPassword(@PathVariable("id") Long id,
                                             @Valid @RequestBody UserResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return ApiResult.success();
    }

    @SaCheckPermission("system:user:edit")
    @DeleteMapping("/{id}")
    @com.edumind.common.annotation.OperationLog(module = "用户管理", title = "删除系统用户", businessType = com.edumind.common.enums.BusinessType.DELETE)
    public ApiResult<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ApiResult.success();
    }
}
