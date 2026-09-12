package com.edumind.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.fastjson2.JSON;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.config.MailConfigDTO;
import com.edumind.system.dto.config.MailTestDTO;
import com.edumind.system.service.config.SysConfigService;
import com.edumind.system.vo.config.MailConfigVO;
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
     * 获取系统基础信息配置
     */
    @GetMapping("/base")
    public ApiResult<Map<String, Object>> getBaseInfo() {
        String json = sysConfigService.getConfigValue("sys.base.info");
        if (json != null && !json.isBlank()) {
            return ApiResult.success(JSON.parseObject(json));
        }
        return ApiResult.success(Map.of(
                "platformName", "智教云 · EduMind",
                "subTitle", "AI 智能教学赋能平台",
                "copyright", "© 2026 EduMind. All rights reserved.",
                "icp", "京ICP备20260001号-1"
        ));
    }
}
