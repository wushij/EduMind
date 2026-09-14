package com.edumind.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.dto.tenant.CampusCreateDTO;
import com.edumind.system.dto.tenant.CampusUpdateDTO;
import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.dto.tenant.TenantUpdateDTO;
import com.edumind.system.vo.tenant.CampusVO;
import com.edumind.system.vo.tenant.SysTenantOverviewStatsVO;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;

import java.util.List;
import java.util.Map;

public interface SysTenantService {

    SysTenantOverviewStatsVO getOverviewStats();

    Page<TenantListVO> pageTenants(int page, int pageSize, String keyword, Integer status);

    TenantDetailVO getTenantDetail(Long tenantId);

    Long createTenant(TenantCreateDTO dto);

    void updateTenant(Long id, TenantUpdateDTO dto);

    void updateTenantStatus(Long id, Integer status);

    void deleteTenant(Long id);

    List<CampusVO> listCampuses(Long tenantId);

    Long createCampus(Long tenantId, CampusCreateDTO dto);

    void updateCampus(Long tenantId, Long campusId, CampusUpdateDTO dto);

    void updateCampusStatus(Long tenantId, Long campusId, Integer status);

    void deleteCampus(Long tenantId, Long campusId);

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
