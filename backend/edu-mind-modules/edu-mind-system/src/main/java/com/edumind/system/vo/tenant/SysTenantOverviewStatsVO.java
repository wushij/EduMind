package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysTenantOverviewStatsVO implements Serializable {
    private Long totalTenants;
    private Long activeTenants;
    private Long totalCampuses;
    private Long totalMembers;
    private Long totalStudents;
    private Long totalTeachers;
    private Double complianceRate;
    private Long totalTokenQuota;
    private Long usedTokenQuota;
}
