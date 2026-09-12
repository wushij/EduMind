package com.edumind.system.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 找回密码身份验证结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyVO implements Serializable {

    /**
     * 重置密码临时安全票据 (TTL 10分钟，一次性有效)
     */
    private String resetToken;
}
