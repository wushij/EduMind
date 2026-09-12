package com.edumind.system.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全链路安全防护矩阵配置更新入参 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityConfigDTO implements Serializable {

    /** 时间戳防重放校验开关 */
    private Boolean timestampEnabled;

    /** 时间戳允许时间窗口偏差(ms) */
    private Long timestampWindowMs;

    /** Nonce 随机数防重放开关 */
    private Boolean nonceEnabled;

    /** 国密 HMAC-SM3 数字签名开关 */
    private Boolean sm3SignEnabled;

    /** 国密 SM4-CBC 接口加解密开关 */
    private Boolean sm4EncryptEnabled;

    /** 登录失败触发图形验证码阈值 (-1:从不, 0:始终, 1:输错1次, 3:输错3次) */
    private Integer captchaAfterFailures;

    /** 注册是否需要图形验证码 */
    private Boolean captchaOnRegister;

    /** 禁止前端调试 (防打开 F12 控制台) */
    private Boolean disableDevtool;
}
