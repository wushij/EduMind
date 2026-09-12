package com.edumind.system.dto.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件 SMTP 系统配置更新入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailConfigDTO implements Serializable {

    /** 是否启用发信服务 */
    @NotNull(message = "请指定是否启用发信服务")
    private Boolean enabled;

    /** SMTP 服务器主机名 */
    @NotBlank(message = "SMTP 服务器地址不能为空")
    private String host;

    /** SMTP 端口 (465 SSL / 587 STARTTLS / 25) */
    @NotNull(message = "SMTP 端口不能为空")
    @Min(value = 1, message = "端口范围不合法")
    @Max(value = 65535, message = "端口范围不合法")
    private Integer port;

    /** 发信邮箱账号 */
    @NotBlank(message = "发件邮箱账号不能为空")
    private String username;

    /** 邮箱授权码 / 密码（若填入 ****** 则代表保持原密码不变） */
    private String password;

    /** 发信人显示名称 */
    @NotBlank(message = "发信人显示名称不能为空")
    private String fromName;

    /** 是否启用 SSL / TLS 加密 */
    @NotNull(message = "请指定是否启用 SSL/TLS")
    private Boolean useSsl;

    /** 验证码有效时间（分钟） */
    @Min(value = 1, message = "有效时间至少 1 分钟")
    @Max(value = 60, message = "有效时间不得超过 60 分钟")
    private Integer codeExpireMinutes;

    /** 发送频率冷却时间（秒） */
    @Min(value = 10, message = "冷却时间至少 10 秒")
    @Max(value = 600, message = "冷却时间不得超过 600 秒")
    private Integer codeIntervalSeconds;

    /** 单邮箱单日最大发送限制（次） */
    @Min(value = 1, message = "单日限额至少 1 次")
    @Max(value = 100, message = "单日限额不得超过 100 次")
    private Integer dailyLimitPerEmail;
}
