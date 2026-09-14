package com.edumind.security.captcha;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 滑块拖拽轨迹校验入参 (对齐 Code Compass SliderVerifyReq)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderVerifyDTO implements Serializable {

    @NotBlank(message = "challengeId 不能为空")
    private String challengeId;

    private int offsetX;

    private int durationMs;

    private List<SliderTrackEvent> events;

    private String username;
}
