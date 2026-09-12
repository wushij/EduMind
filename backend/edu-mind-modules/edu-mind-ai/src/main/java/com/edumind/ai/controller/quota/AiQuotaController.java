package com.edumind.ai.controller.quota;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.quota.SysAiQuotaUpdateDTO;
import com.edumind.ai.service.quota.AiQuotaManageService;
import com.edumind.ai.vo.quota.SysAiQuotaVO;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/ai-quota")
@RequiredArgsConstructor
public class AiQuotaController {

    private final AiQuotaManageService aiQuotaManageService;

    @SaCheckPermission("system:quota:view")
    @GetMapping
    public ApiResult<List<SysAiQuotaVO>> list() {
        return ApiResult.success(aiQuotaManageService.listAll());
    }

    @SaCheckPermission("system:quota:edit")
    @PutMapping("/{userId}")
    public ApiResult<Void> update(@PathVariable Long userId, @Valid @RequestBody SysAiQuotaUpdateDTO body) {
        aiQuotaManageService.updateUserQuota(userId, body);
        return ApiResult.success();
    }
}
