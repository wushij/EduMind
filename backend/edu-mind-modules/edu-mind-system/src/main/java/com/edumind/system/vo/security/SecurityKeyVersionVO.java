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

    /**
     * 派生密钥安全指纹 (SHA-256 前缀，如 SM4#7F8A-3C2B...)
     */
    private String keyFingerprint;

    /**
     * 关联业务场景描述
     */
    private String usageScope;

    /**
     * 激活生效时间
     */
    private LocalDateTime activatedTime;

    /**
     * 创建记录时间
     */
    private LocalDateTime createTime;
}

