package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 成员所属组织跨模块只读简要信息（API 契约层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberOrgBriefVO implements Serializable {
    private Long id;
    private String name;
    private String type;
    private String roleType;
}
