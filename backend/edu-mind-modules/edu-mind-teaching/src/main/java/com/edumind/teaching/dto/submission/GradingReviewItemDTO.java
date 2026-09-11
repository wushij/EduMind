package com.edumind.teaching.dto.submission;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class GradingReviewItemDTO implements Serializable {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "分数不能为空")
    private Integer score;

    private String teacherComment;
}
