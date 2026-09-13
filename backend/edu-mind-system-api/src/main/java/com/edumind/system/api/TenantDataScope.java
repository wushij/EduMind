package com.edumind.system.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 租户五级数据范围解析模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDataScope implements Serializable {

    /**
     * 是否具备当前租户全局数据范围 (平台超管 / 租户管理员)
     */
    private boolean allTenant;

    /**
     * 是否平台代管会话
     */
    private boolean platformDelegated;

    /**
     * 是否具有配额管理权限 (系统/校级配额)
     */
    private boolean canManageQuota;

    /**
     * 可见的组织机构节点 ID 集合 (院系管理员 / 教师 / 学生)
     */
    @Builder.Default
    private Set<Long> orgIds = new HashSet<>();

    /**
     * 可见的直接课程 ID 集合 (教师所授课程 / 学生所选课程 / 院系关联课程)
     */
    @Builder.Default
    private Set<Long> courseIds = new HashSet<>();

    public static TenantDataScope fullTenant(boolean platformDelegated) {
        return TenantDataScope.builder()
                .allTenant(true)
                .platformDelegated(platformDelegated)
                .canManageQuota(true)
                .orgIds(Collections.emptySet())
                .courseIds(Collections.emptySet())
                .build();
    }
}
