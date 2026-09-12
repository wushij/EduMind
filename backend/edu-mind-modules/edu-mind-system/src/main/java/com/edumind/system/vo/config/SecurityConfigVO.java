package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全链路安全防护矩阵配置视图模型 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityConfigVO implements Serializable {

    private boolean timestampEnabled;
    private long timestampWindowMs;
    private boolean nonceEnabled;
    private boolean sm3SignEnabled;
    private boolean sm4EncryptEnabled;
    private int captchaAfterFailures;
    private boolean captchaOnRegister;
    private boolean disableDevtool;
}
