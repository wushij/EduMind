package com.edumind.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class RegisterDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String captcha;

    private String captchaId;

    private String captchaKey;

    private String captchaCode;

    public String resolveCaptchaId() {
        return captchaId != null ? captchaId : captchaKey;
    }

    public String resolveCaptcha() {
        return captcha != null ? captcha : captchaCode;
    }
}
