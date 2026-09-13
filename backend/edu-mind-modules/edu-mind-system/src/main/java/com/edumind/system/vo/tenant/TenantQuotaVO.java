package com.edumind.system.vo.tenant;

import lombok.Data;

@Data
public class TenantQuotaVO {
    private Long id;
    private Long tenantId;
    private String quotaType;
    private Long limitValue;
    private Long usedValue;
    private Integer usagePercent;
    private Integer warningThreshold;
    private boolean isWarning;
}
