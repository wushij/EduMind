package com.edumind.security.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 验证码系统策略响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaPolicyVO implements Serializable {
    private boolean captchaEnabled;
    private String captchaType; // "image" | "slider"
    private boolean smsLoginSliderCaptchaEnabled;
    private boolean emailLoginSliderCaptchaEnabled;
}
