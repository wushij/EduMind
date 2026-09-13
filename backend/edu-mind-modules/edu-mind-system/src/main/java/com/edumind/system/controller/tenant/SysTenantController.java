package com.edumind.system.controller.tenant;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.ApiResult;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.service.SysTenantService;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/tenants")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService sysTenantService;

    @GetMapping("/current")
    public ApiResult<TenantDetailVO> getCurrentTenant() {
        return ApiResult.success(sysTenantService.getCurrentTenantInfo());
    }

    @GetMapping("/available")
    public ApiResult<List<TenantListVO>> getAvailableTenants() {
        Long userId = LoginUserResolver.requireUserId();
        return ApiResult.success(sysTenantService.listUserAvailableTenants(userId));
    }

    @PostMapping("/switch")
    public ApiResult<Map<String, Object>> switchTenant(@Valid @RequestBody TenantSwitchDTO dto) {
        return ApiResult.success(sysTenantService.switchTenant(dto));
    }

    @SaCheckRole("ADMIN")
    @GetMapping
    public ApiResult<Page<TenantListVO>> pageTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResult.success(sysTenantService.pageTenants(page, pageSize, keyword, status));
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/{id}")
    public ApiResult<TenantDetailVO> getTenantDetail(@PathVariable("id") Long id) {
        return ApiResult.success(sysTenantService.getTenantDetail(id));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public ApiResult<Long> createTenant(@Valid @RequestBody TenantCreateDTO dto) {
        return ApiResult.success(sysTenantService.createTenant(dto));
    }
}
