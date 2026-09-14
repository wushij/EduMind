package com.edumind.security.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 获取滑块拼图挑战入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderChallengeDTO implements Serializable {
    private String operation;
    private String username;
}
