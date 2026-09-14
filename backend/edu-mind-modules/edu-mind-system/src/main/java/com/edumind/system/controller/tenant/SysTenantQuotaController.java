package com.edumind.system.controller.tenant;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
import com.edumind.system.dto.tenant.OrgQuotaUpdateDTO;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.service.quota.OrgQuotaService;
import com.edumind.system.service.quota.TenantQuotaService;
import com.edumind.system.vo.tenant.OrgQuotaVO;
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
    private final OrgQuotaService orgQuotaService;

    @GetMapping
    @SaCheckPermission(value = {"system:quota:view", "system:quota:edit", "system:user:view", "admin"}, mode = SaMode.OR)
    public ApiResult<List<TenantQuotaVO>> listQuotas(@RequestParam(required = false) Long tenantId) {
        return ApiResult.success(tenantQuotaService.listQuotas(tenantId));
    }

    @PutMapping
    @SaCheckPermission(value = {"system:quota:edit", "admin"}, mode = SaMode.OR)
    @OperationLog(module = "租户配额", title = "调整租户配额阈值", businessType = BusinessType.UPDATE)
    public ApiResult<Void> updateQuotaThreshold(@Valid @RequestBody QuotaUpdateDTO dto) {
        tenantQuotaService.updateQuotaThreshold(dto);
        return ApiResult.success();
    }

    @GetMapping("/organizations")
    @SaCheckPermission(value = {"system:quota:view", "system:quota:edit", "system:user:view", "admin"}, mode = SaMode.OR)
    public ApiResult<List<OrgQuotaVO>> listOrgQuotas(@RequestParam(required = false) Long tenantId) {
        return ApiResult.success(orgQuotaService.listOrgQuotas(tenantId));
    }

    @PutMapping("/organizations")
    @SaCheckPermission(value = {"system:quota:edit", "admin"}, mode = SaMode.OR)
    @OperationLog(module = "租户配额", title = "调整组织配额", businessType = BusinessType.UPDATE)
    public ApiResult<Void> updateOrgQuota(@Valid @RequestBody OrgQuotaUpdateDTO dto) {
        orgQuotaService.updateOrgQuota(dto);
        return ApiResult.success();
    }
}
