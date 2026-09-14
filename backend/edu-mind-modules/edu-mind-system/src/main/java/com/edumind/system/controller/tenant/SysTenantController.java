package com.edumind.system.controller.tenant;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.ApiResult;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.dto.tenant.CampusCreateDTO;
import com.edumind.system.dto.tenant.CampusUpdateDTO;
import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.dto.tenant.TenantUpdateDTO;
import com.edumind.system.service.SysTenantService;
import com.edumind.system.vo.tenant.CampusVO;
import com.edumind.system.vo.tenant.SysTenantOverviewStatsVO;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @GetMapping("/stats")
    public ApiResult<SysTenantOverviewStatsVO> getOverviewStats() {
        return ApiResult.success(sysTenantService.getOverviewStats());
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

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    public ApiResult<Void> updateTenant(@PathVariable("id") Long id, @Valid @RequestBody TenantUpdateDTO dto) {
        sysTenantService.updateTenant(id, dto);
        return ApiResult.success();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    public ApiResult<Void> updateTenantStatus(@PathVariable("id") Long id, @RequestParam Integer status) {
        sysTenantService.updateTenantStatus(id, status);
        return ApiResult.success();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteTenant(@PathVariable("id") Long id) {
        sysTenantService.deleteTenant(id);
        return ApiResult.success();
    }

    // --- 校区管理接口 ---

    @SaCheckRole("ADMIN")
    @GetMapping("/{id}/campuses")
    public ApiResult<List<CampusVO>> listCampuses(@PathVariable("id") Long id) {
        return ApiResult.success(sysTenantService.listCampuses(id));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{id}/campuses")
    public ApiResult<Long> createCampus(@PathVariable("id") Long id, @Valid @RequestBody CampusCreateDTO dto) {
        return ApiResult.success(sysTenantService.createCampus(id, dto));
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/campuses/{campusId}")
    public ApiResult<Void> updateCampus(
            @PathVariable("id") Long id,
            @PathVariable("campusId") Long campusId,
            @Valid @RequestBody CampusUpdateDTO dto) {
        sysTenantService.updateCampus(id, campusId, dto);
        return ApiResult.success();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/campuses/{campusId}/status")
    public ApiResult<Void> updateCampusStatus(
            @PathVariable("id") Long id,
            @PathVariable("campusId") Long campusId,
            @RequestParam Integer status) {
        sysTenantService.updateCampusStatus(id, campusId, status);
        return ApiResult.success();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}/campuses/{campusId}")
    public ApiResult<Void> deleteCampus(
            @PathVariable("id") Long id,
            @PathVariable("campusId") Long campusId) {
        sysTenantService.deleteCampus(id, campusId);
        return ApiResult.success();
    }
}
