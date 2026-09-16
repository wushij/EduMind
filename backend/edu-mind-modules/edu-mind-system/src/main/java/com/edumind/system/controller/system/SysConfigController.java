package com.edumind.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.fastjson2.JSON;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.config.MailConfigDTO;
import com.edumind.system.dto.config.MailTestDTO;
import com.edumind.system.dto.config.SecurityConfigDTO;
import com.edumind.system.dto.config.StorageConfigDTO;
import com.edumind.system.service.config.SysConfigService;
import com.edumind.system.vo.config.MailConfigVO;
import com.edumind.system.vo.config.SecurityConfigVO;
import com.edumind.system.vo.config.StorageConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统全局参数配置控制器
 */
@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 获取邮件服务 SMTP 配置
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/mail")
    public ApiResult<MailConfigVO> getMailConfig() {
        return ApiResult.success(sysConfigService.getMailConfigVO());
    }

    /**
     * 更新邮件服务 SMTP 配置
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/mail")
    public ApiResult<Void> updateMailConfig(@Valid @RequestBody MailConfigDTO dto) {
        sysConfigService.updateMailConfig(dto);
        return ApiResult.success();
    }

    /**
     * 测试发信服务连通性
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/mail/test")
    public ApiResult<Void> testMail(@Valid @RequestBody MailTestDTO dto) {
        sysConfigService.testMail(dto);
        return ApiResult.success();
    }

    /**
     * 获取系统文件存储配置
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/storage")
    public ApiResult<StorageConfigVO> getStorageConfig() {
        return ApiResult.success(sysConfigService.getStorageConfigVO());
    }

    /**
     * 更新文件存储配置并热重载存储引擎
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/storage")
    public ApiResult<Void> updateStorageConfig(@Valid @RequestBody StorageConfigDTO dto) {
        sysConfigService.updateStorageConfig(dto);
        return ApiResult.success();
    }

    /**
     * 测试指定文件存储配置的连通性
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/storage/test")
    public ApiResult<Void> testStorageConfig(@Valid @RequestBody StorageConfigDTO dto) {
        sysConfigService.testStorageConfig(dto);
        return ApiResult.success();
    }

    /**
     * 获取系统网络安全配置
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/security")
    public ApiResult<SecurityConfigVO> getSecurityConfig() {
        return ApiResult.success(sysConfigService.getSecurityConfigVO());
    }

    /**
     * 更新网络安全配置并热生效
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/security")
    public ApiResult<Void> updateSecurityConfig(@Valid @RequestBody SecurityConfigDTO dto) {
        sysConfigService.updateSecurityConfig(dto);
        return ApiResult.success();
    }

    /**
     * 获取系统基础信息配置
     */
    @GetMapping("/base")
    public ApiResult<Map<String, Object>> getBaseInfo() {
        com.edumind.system.vo.config.SysConfigGroupVO group = sysConfigService.getByGroupCode("site");
        if (group != null && group.getConfigValue() != null && !group.getConfigValue().isBlank()) {
            return ApiResult.success(JSON.parseObject(group.getConfigValue()));
        }
        return ApiResult.success(Map.of(
                "platformName", "EduMind",
                "subTitle", "智教云 · EduMind",
                "copyright", "Copyright © 2026 EduMind. All rights reserved.",
                "icp", "京ICP备20260001号-1"
        ));
    }

    /**
     * 获取全部系统配置分组列表
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/groups")
    public ApiResult<java.util.List<com.edumind.system.vo.config.SysConfigGroupVO>> listAllGroups() {
        return ApiResult.success(sysConfigService.listAllGroups());
    }

    /**
     * 获取指定配置分组
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/groups/{groupCode}")
    public ApiResult<com.edumind.system.vo.config.SysConfigGroupVO> getGroup(@org.springframework.web.bind.annotation.PathVariable String groupCode) {
        return ApiResult.success(sysConfigService.getByGroupCode(groupCode));
    }

    /**
     * 更新指定配置分组
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/groups/{groupCode}")
    public ApiResult<Void> updateGroup(
            @org.springframework.web.bind.annotation.PathVariable String groupCode,
            @RequestBody com.edumind.system.dto.config.SysConfigGroupDTO dto) {
        sysConfigService.updateConfigGroup(groupCode, dto.getConfigValue());
        return ApiResult.success();
    }

    /**
     * 测试发送短信
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/test-sms")
    public ApiResult<Boolean> testSms(@Valid @RequestBody com.edumind.system.dto.config.TestSmsDTO dto) {
        return ApiResult.success(sysConfigService.testSms(dto));
    }

    /**
     * 获取最近短信发送记录
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/sms-logs/recent")
    public ApiResult<java.util.List<com.edumind.system.vo.config.SmsLogVO>> getRecentSmsLogs(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") Integer limit) {
        return ApiResult.success(sysConfigService.getRecentSmsLogs(limit));
    }

    /**
     * 分页查询短信发送记录
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/sms-logs")
    public ApiResult<com.edumind.common.api.PageResult<com.edumind.system.vo.config.SmsLogVO>> pageSmsLogs(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") Integer size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String phone,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer status) {
        return ApiResult.success(sysConfigService.pageSmsLogs(page, size, phone, status));
    }

    /**
     * 创建测试支付订单
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/test-payment")
    public ApiResult<java.util.Map<String, String>> testPayment(
            @Valid @RequestBody com.edumind.system.dto.config.TestPaymentDTO dto) {
        return ApiResult.success(sysConfigService.testPayment(dto));
    }

    /**
     * 获取最近邮件发送记录
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/email-logs/recent")
    public ApiResult<java.util.List<com.edumind.system.vo.config.EmailLogVO>> getRecentEmailLogs(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") Integer limit) {
        return ApiResult.success(sysConfigService.getRecentEmailLogs(limit));
    }

    /**
     * 分页查询邮件发送记录
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/email-logs")
    public ApiResult<com.edumind.common.api.PageResult<com.edumind.system.vo.config.EmailLogVO>> pageEmailLogs(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") Integer size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer status) {
        return ApiResult.success(sysConfigService.pageEmailLogs(page, size, email, status));
    }

    /**
     * 获取可用角色选项
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/role-options")
    public ApiResult<java.util.List<com.edumind.system.vo.config.RoleOptionVO>> getRoleOptions() {
        return ApiResult.success(sysConfigService.getRoleOptions());
    }

    /**
     * 获取可用用户选项
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/user-options")
    public ApiResult<java.util.List<com.edumind.system.vo.config.UserOptionVO>> getUserOptions() {
        return ApiResult.success(sysConfigService.getUserOptions());
    }
}
