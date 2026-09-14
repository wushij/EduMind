package com.edumind.system.dto.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 国密 SM4-GCM 在线加解密实机测试请求 DTO
 */
@Data
public class SecurityKeyCryptoTestDTO implements Serializable {

    /**
     * 密钥别名 (如 edumind-data-key 或 edumind-model-key)
     */
    private String keyAlias;

    /**
     * 指定密钥版本号 (可选，默认使用当前 ACTIVE 活跃版本)
     */
    private Integer keyVersion;

    /**
     * 操作类型: ENCRYPT (加密) / DECRYPT (解密)
     */
    @NotBlank(message = "操作类型不能为空")
    private String operation;

    /**
     * 输入文本 (明文待加密，或 Base64 密文待解密)
     */
    @NotBlank(message = "测试输入文本不能为空")
    private String text;
}
