package com.edumind.system.controller.auth;

import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.auth.LoginDTO;
import com.edumind.system.dto.auth.RegisterDTO;
import com.edumind.system.service.auth.AuthService;
import com.edumind.system.vo.auth.LoginVO;
import com.edumind.system.vo.user.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiResult.success(authService.login(loginDTO));
    }

    @PostMapping("/email-login")
    public ApiResult<LoginVO> emailLogin(@Valid @RequestBody com.edumind.system.dto.auth.EmailLoginDTO emailLoginDTO) {
        return ApiResult.success(authService.emailLogin(emailLoginDTO));
    }

    @PostMapping("/send-email-code")
    public ApiResult<Void> sendEmailCode(@Valid @RequestBody com.edumind.system.dto.auth.EmailSendCodeDTO sendCodeDTO) {
        authService.sendEmailCode(sendCodeDTO);
        return ApiResult.success();
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Long>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Long userId = authService.register(registerDTO);
        return ApiResult.success(Map.of("userId", userId));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.success();
    }

    @GetMapping("/user-info")
    public ApiResult<UserVO> getUserInfo() {
        return ApiResult.success(authService.getUserInfo());
    }
}
