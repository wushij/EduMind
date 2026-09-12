package com.edumind.system.controller.user;

import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.user.PasswordChangeDTO;
import com.edumind.system.dto.user.UserProfileUpdateDTO;
import com.edumind.system.service.user.UserService;
import com.edumind.system.vo.user.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users/profile")
    public ApiResult<UserVO> getProfile() {
        return ApiResult.success(userService.getProfile());
    }

    @PutMapping("/api/users/profile")
    public ApiResult<UserVO> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        return ApiResult.success(userService.updateProfile(dto));
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/users/avatar")
    public ApiResult<UserVO> uploadAvatar(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResult.success(userService.uploadAvatar(file));
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/users/send-bind-code")
    public ApiResult<Void> sendBindCode(@Valid @RequestBody com.edumind.system.dto.auth.EmailSendCodeDTO dto) {
        userService.sendBindEmailCode(dto.getEmail());
        return ApiResult.success();
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/users/bind-email")
    public ApiResult<UserVO> bindEmail(@Valid @RequestBody com.edumind.system.dto.user.EmailBindDTO dto) {
        return ApiResult.success(userService.bindEmail(dto));
    }

    @PutMapping("/api/auth/password")
    public ApiResult<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(dto);
        return ApiResult.success();
    }
}
