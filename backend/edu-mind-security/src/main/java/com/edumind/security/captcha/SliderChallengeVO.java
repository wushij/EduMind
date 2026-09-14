package com.edumind.security.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 滑块拼图挑战展示模型 (对齐 Code Compass SliderChallengeDTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderChallengeVO implements Serializable {
    private String challengeId;
    private String background;
    private String piece;
    private int width;
    private int height;
    private int pieceSize;
    private int pieceY;
    private long expireAt;
}
