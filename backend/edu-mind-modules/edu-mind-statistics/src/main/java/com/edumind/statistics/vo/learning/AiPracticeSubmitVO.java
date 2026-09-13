package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class AiPracticeSubmitVO {
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracyRate;
    private Integer durationSeconds;
}
