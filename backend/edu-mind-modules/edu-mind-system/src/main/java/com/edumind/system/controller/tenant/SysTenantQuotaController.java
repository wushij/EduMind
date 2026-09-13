package com.edumind.system.controller.tenant;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.service.quota.TenantQuotaService;
import com.edumind.system.vo.tenant.TenantQuotaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/tenant-quotas")
@RequiredArgsConstructor
public class SysTenantQuotaController {

    private final TenantQuotaService tenantQuotaService;

    @GetMapping
    @SaCheckPermission(value = {"system:quota:view", "system:quota:edit", "system:user:view", "admin"}, mode = SaMode.OR)
    public ApiResult<List<TenantQuotaVO>> listQuotas(@RequestParam(required = false) Long tenantId) {
        return ApiResult.success(tenantQuotaService.listQuotas(tenantId));
    }

    @PutMapping
    @SaCheckPermission(value = {"system:quota:edit", "admin"}, mode = SaMode.OR)
    public ApiResult<Void> updateQuotaThreshold(@Valid @RequestBody QuotaUpdateDTO dto) {
        tenantQuotaService.updateQuotaThreshold(dto);
        return ApiResult.success();
    }
}
