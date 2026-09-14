package com.edumind.system.dto.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 国密密钥版本轮换请求 DTO
 */
@Data
public class SecurityKeyRotateDTO implements Serializable {

    /**
     * 可选的目标密钥记录 ID (若传入则进行 IDOR 租户归属校验)
     */
    private Long id;

    /**
     * 密钥别名 (如 edumind-data-key)
     */
    @NotBlank(message = "密钥别名不能为空")
    private String keyAlias;
}
