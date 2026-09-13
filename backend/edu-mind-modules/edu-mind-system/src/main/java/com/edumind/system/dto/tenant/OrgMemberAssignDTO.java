package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织成员分配 DTO
 */
@Data
public class OrgMemberAssignDTO {

    /**
     * 租户成员 ID (对应 sys_tenant_member.id)
     */
    @NotNull(message = "成员ID不能为空")
    private Long memberId;

    /**
     * 组织内角色 (HEAD_TEACHER / TEACHER / STUDENT)，默认为 STUDENT
     */
    private String roleType;
}
