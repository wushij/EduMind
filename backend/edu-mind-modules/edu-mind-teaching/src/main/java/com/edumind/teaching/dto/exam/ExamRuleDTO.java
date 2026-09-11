package com.edumind.teaching.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamRuleDTO {
    @NotNull(message = "题型不能为空")
    private String type;
    @NotNull(message = "题目数量不能为空")
    private Integer count;
    @NotNull(message = "每题分值不能为空")
    private Integer scoreEach;
}
