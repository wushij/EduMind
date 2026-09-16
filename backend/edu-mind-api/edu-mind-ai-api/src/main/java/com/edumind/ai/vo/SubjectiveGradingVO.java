package com.edumind.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectiveGradingVO {
    private Integer score;
    private Integer maxScore;
    private String aiComment;
    private String status;
}
