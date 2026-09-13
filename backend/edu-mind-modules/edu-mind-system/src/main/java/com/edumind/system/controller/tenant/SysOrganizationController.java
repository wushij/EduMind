package com.edumind.system.controller.tenant;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.common.api.ApiResult;
import com.edumind.common.context.TenantContext;
import com.edumind.system.dto.tenant.OrgCreateDTO;
import com.edumind.system.dto.tenant.OrgUpdateDTO;
import com.edumind.system.service.SysOrganizationService;
import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/organizations")
@RequiredArgsConstructor
public class SysOrganizationController {

    private final SysOrganizationService sysOrganizationService;

    @GetMapping("/tree")
    @SaCheckPermission(value = {"system:organization:list", "system:organization:view", "system:user:view"}, mode = SaMode.OR)
    public ApiResult<List<OrganizationNodeVO>> getTree() {
        Long tenantId = TenantContext.requireTenantId();
        return ApiResult.success(sysOrganizationService.getTree(tenantId));
    }

    @GetMapping("/{id}/members")
    @SaCheckPermission(value = {"system:organization:list", "system:organization:view", "system:user:view"}, mode = SaMode.OR)
    public ApiResult<List<OrganizationMemberVO>> getOrgMembers(@PathVariable("id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return ApiResult.success(sysOrganizationService.getOrgMembers(tenantId, id));
    }

    @PostMapping("/{id}/members")
    @SaCheckPermission(value = {"system:organization:assign", "system:organization:edit", "system:user:edit"}, mode = SaMode.OR)
    public ApiResult<Void> assignMember(@PathVariable("id") Long id, @Valid @RequestBody com.edumind.system.dto.tenant.OrgMemberAssignDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        sysOrganizationService.assignMember(tenantId, id, dto);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @SaCheckPermission(value = {"system:organization:assign", "system:organization:edit", "system:user:edit"}, mode = SaMode.OR)
    public ApiResult<Void> removeMember(@PathVariable("id") Long id, @PathVariable("memberId") Long memberId) {
        Long tenantId = TenantContext.requireTenantId();
        sysOrganizationService.removeMember(tenantId, id, memberId);
        return ApiResult.success();
    }

    @PostMapping
    @SaCheckPermission(value = {"system:organization:create", "system:organization:edit", "system:user:edit"}, mode = SaMode.OR)
    public ApiResult<Long> createNode(@Valid @RequestBody OrgCreateDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        return ApiResult.success(sysOrganizationService.createNode(
                tenantId, dto.getName(), dto.getOrgType(), dto.getParentId(), dto.getSortOrder()));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(value = {"system:organization:update", "system:organization:edit", "system:user:edit"}, mode = SaMode.OR)
    public ApiResult<Void> updateNode(@PathVariable("id") Long id, @Valid @RequestBody OrgUpdateDTO dto) {
        sysOrganizationService.updateNode(id, dto.getName(), dto.getSortOrder());
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(value = {"system:organization:delete", "system:organization:edit", "system:user:edit"}, mode = SaMode.OR)
    public ApiResult<Void> deleteNode(@PathVariable("id") Long id) {
        sysOrganizationService.deleteNode(id);
        return ApiResult.success();
    }
}
