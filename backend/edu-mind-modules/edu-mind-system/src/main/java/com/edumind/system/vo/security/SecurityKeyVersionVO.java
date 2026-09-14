package com.edumind.system.vo.security;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 国密密钥版本展示视图对象 VO
 */
@Data
public class SecurityKeyVersionVO implements Serializable {

    private Long id;

    private Long tenantId;

    private String keyAlias;

    private Integer keyVersion;

    /**
     * 算法名称展示 (如 SM4-GCM)
     */
    private String algorithm;

    /**
     * 状态 (ACTIVE / ROTATING / DEPRECATED)
     */
    private String status;

    private LocalDateTime activatedTime;
}
