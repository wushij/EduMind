package com.edumind.system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发送邮箱验证码入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendCodeDTO implements Serializable {

    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "请输入有效的电子邮箱格式")
    private String email;

    /** 业务场景：login / bind / resetpwd */
    @Builder.Default
    private String scene = "login";

    /** 图形验证码凭据ID（防人机刷码可选校验） */
    private String captchaId;

    /** 图形验证码结果 */
    private String captcha;
}
