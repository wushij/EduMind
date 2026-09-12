package com.edumind.infrastructure.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SMTP 邮件发信与验证码策略配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailConfig implements Serializable {

    /** 是否启用发信服务 */
    @Builder.Default
    private Boolean enabled = false;

    /** SMTP 服务器主机名 (如 smtp.qq.com / smtp.163.com) */
    @Builder.Default
    private String host = "smtp.qq.com";

    /** SMTP 端口 (465 SSL, 587 STARTTLS, 25 普通) */
    @Builder.Default
    private Integer port = 465;

    /** 发信邮箱账号 */
    private String username;

    /** 邮箱授权码 / 密码 */
    private String password;

    /** 发信人显示名称 */
    @Builder.Default
    private String fromName = "智教云 · EduMind";

    /** 是否启用 SSL / TLS 加密 */
    @Builder.Default
    private Boolean useSsl = true;

    /** 验证码有效时间（分钟） */
    @Builder.Default
    private Integer codeExpireMinutes = 5;

    /** 发送频率冷却时间（秒） */
    @Builder.Default
    private Integer codeIntervalSeconds = 60;

    /** 单邮箱单日最大发送限制（次） */
    @Builder.Default
    private Integer dailyLimitPerEmail = 10;
}
