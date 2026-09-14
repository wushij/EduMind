package com.edumind.security.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 滑块拖动轨迹瞬时点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderTrackEvent implements Serializable {
    private int x;
    private int y;
    private int t;
}
