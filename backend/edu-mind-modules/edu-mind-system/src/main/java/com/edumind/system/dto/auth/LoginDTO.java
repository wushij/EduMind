package com.edumind.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求传输对象
 */
@Data
public class LoginDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captcha;

    private String captchaId;

    /** 滑块验证成功凭证 (一次性 Token) */
    private String captchaToken;

    /** 前端契约字段别名 */
    private String captchaKey;

    private String captchaCode;

    private String role;

    public String resolveCaptchaId() {
        return captchaId != null ? captchaId : captchaKey;
    }

    public String resolveCaptcha() {
        return captcha != null ? captcha : captchaCode;
    }
}
