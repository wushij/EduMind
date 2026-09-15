package com.edumind.system.service.tenant;

import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;

import java.util.List;
import java.util.Map;

public interface TenantSessionService {

    List<TenantListVO> listUserAvailableTenants(Long userId);

    TenantDetailVO getCurrentTenantInfo();

    Long initializeLoginTenantSession(Long userId);

    Map<String, Object> switchTenant(TenantSwitchDTO dto);
}
