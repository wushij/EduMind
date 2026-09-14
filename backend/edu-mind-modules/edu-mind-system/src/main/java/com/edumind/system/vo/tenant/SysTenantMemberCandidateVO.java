package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 待分配组织成员候选人 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysTenantMemberCandidateVO implements Serializable {
    private Long memberId;
    private Long userId;
    private String memberNo;
    private String realName;
    private String username;
    private String avatar;
    private String phone;
    private Boolean isAssigned;
    private String currentRole;
}
