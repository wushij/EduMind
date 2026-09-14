package com.edumind.system.vo.security;

import lombok.Data;

import java.io.Serializable;

/**
 * 国密 SM4-GCM 在线加解密实机测试结果 VO
 */
@Data
public class SecurityKeyCryptoTestVO implements Serializable {

    private String keyAlias;

    private Integer keyVersion;

    private String algorithm;

    private String operation;

    private String resultText;

    private Long durationMs;

    private boolean success;

    private String message;
}
