package com.edumind.security.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 滑块校验成功颁发凭证 (对齐 Code Compass SliderVerifyDTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderVerifyVO implements Serializable {
    private String captchaToken;
    private long expireAt;
}
