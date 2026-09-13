package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 租户配额视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantQuotaVO implements Serializable {
    private Long id;
    private Long tenantId;
    private String quotaType;
    private Long limitValue;
    private Long usedValue;
    private Integer usagePercent;
    private Integer warningThreshold;
    private boolean isWarning;
}
