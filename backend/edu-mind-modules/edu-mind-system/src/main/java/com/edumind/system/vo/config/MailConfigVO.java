package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件系统配置展示对象（密码已脱敏掩码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailConfigVO implements Serializable {

    private Boolean enabled;
    private String host;
    private Integer port;
    private String username;
    /** 脱敏掩码展示，如 ****** 或空 */
    private String password;
    /** 是否已配置过有效密码 */
    private Boolean hasPassword;
    private String fromName;
    private Boolean useSsl;
    private Integer codeExpireMinutes;
    private Integer codeIntervalSeconds;
    private Integer dailyLimitPerEmail;
}
