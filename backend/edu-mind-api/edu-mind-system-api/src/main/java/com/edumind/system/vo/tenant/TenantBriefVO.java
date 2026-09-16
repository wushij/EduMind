package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 租户跨模块只读简要信息（API 契约层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBriefVO implements Serializable {
    private Long id;
    private String code;
    private String name;
    private String logo;
    private Integer status;
}
