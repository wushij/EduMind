package com.edumind.system.controller.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.service.security.SecurityKeyVersionService;
import com.edumind.system.vo.security.SecurityKeyVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 国密 KMS 密钥版本管理 Controller
 */
@RestController
@RequestMapping("/api/system/security/keys")
@RequiredArgsConstructor
public class SecurityKeyVersionController {

    private final SecurityKeyVersionService securityKeyVersionService;

    @GetMapping
    @SaCheckPermission(value = {"security:key:view", "admin"}, mode = SaMode.OR)
    public ApiResult<List<SecurityKeyVersionVO>> listKeyVersions(@RequestParam(required = false) String keyAlias) {
        return ApiResult.success(securityKeyVersionService.listKeyVersions(keyAlias));
    }

    @GetMapping("/active")
    @SaCheckPermission(value = {"security:key:view", "admin"}, mode = SaMode.OR)
    public ApiResult<SecurityKeyVersionVO> getActiveKey(@RequestParam(required = false) String keyAlias) {
        return ApiResult.success(securityKeyVersionService.getActiveKey(keyAlias));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(value = {"security:key:view", "admin"}, mode = SaMode.OR)
    public ApiResult<SecurityKeyVersionVO> getKeyVersion(@PathVariable Long id) {
        return ApiResult.success(securityKeyVersionService.getKeyVersion(id));
    }

    @PostMapping("/rotate")
    @SaCheckPermission(value = {"security:key:rotate", "admin"}, mode = SaMode.OR)
    public ApiResult<SecurityKeyVersionVO> rotateKey(@RequestBody(required = false) SecurityKeyRotateDTO dto) {
        SecurityKeyRotateDTO rotateDTO = dto != null ? dto : new SecurityKeyRotateDTO();
        return ApiResult.success(securityKeyVersionService.rotateKey(rotateDTO));
    }
}
