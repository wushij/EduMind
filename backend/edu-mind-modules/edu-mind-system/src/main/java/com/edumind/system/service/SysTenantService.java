package com.edumind.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;

import java.util.List;
import java.util.Map;

public interface SysTenantService {

    Page<TenantListVO> pageTenants(int page, int pageSize, String keyword, Integer status);

    TenantDetailVO getTenantDetail(Long tenantId);

    Long createTenant(TenantCreateDTO dto);

    List<TenantListVO> listUserAvailableTenants(Long userId);

    TenantDetailVO getCurrentTenantInfo();

    Map<String, Object> switchTenant(TenantSwitchDTO dto);

    /**
     * 登录成功后初始化当前会话的租户上下文（优先默认成员租户，管理员无成员时可代管默认租户）
     *
     * @return 已绑定的租户 ID，未绑定则返回 null
     */
    Long initializeLoginTenantSession(Long userId);
}
