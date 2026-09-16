package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 组织架构树节点跨模块只读简要信息（API 契约层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBriefVO implements Serializable {
    private Long id;
    private String name;
    private String orgType;
    private Integer memberCount;
}
