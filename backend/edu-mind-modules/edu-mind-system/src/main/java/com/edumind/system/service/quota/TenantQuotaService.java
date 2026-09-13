package com.edumind.system.service.quota;

import com.edumind.system.api.TenantQuotaApi;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.vo.tenant.TenantQuotaVO;

import java.util.List;

/**
 * 租户配额管理服务接口
 */
public interface TenantQuotaService extends TenantQuotaApi {

    /**
     * 查询指定租户配额列表 (带安全防护校验)
     *
     * @param requestTenantId 客户端请求租户 ID (非平台超管将被重写为当前租户)
     * @return 配额列表
     */
    List<TenantQuotaVO> listQuotas(Long requestTenantId);

    /**
     * 更新配额上限与预警阈值 (带安全防护校验)
     *
     * @param dto 配额更新 DTO
     */
    void updateQuotaThreshold(QuotaUpdateDTO dto);
}
