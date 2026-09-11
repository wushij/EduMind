package com.edumind.teaching.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamGenerateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private String title;
    @NotNull(message = "总分不能为空")
    private Integer totalScore;
    private Integer durationMinutes;
    private List<Long> chapterIds;

    @NotEmpty(message = "组卷规则不能为空")
    private List<ExamRuleDTO> rules;
}
