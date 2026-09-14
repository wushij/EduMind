package com.edumind.system.service.quota;

import com.edumind.system.dto.tenant.OrgQuotaUpdateDTO;
import com.edumind.system.vo.tenant.OrgQuotaVO;

import java.util.List;

/**
 * 组织院系算力配额服务
 */
public interface OrgQuotaService {

    /**
     * 查询租户下所有组织院系的算力配额与用量情况
     */
    List<OrgQuotaVO> listOrgQuotas(Long tenantId);

    /**
     * 更新指定组织院系的算力配额策略
     */
    void updateOrgQuota(OrgQuotaUpdateDTO dto);
}
