package com.edumind.teaching.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamQuestionItemDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    @NotNull(message = "分值不能为空")
    private Integer score;
    private Integer sortOrder;
}
