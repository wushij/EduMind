package com.edumind.teaching.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamCreateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotBlank(message = "试卷标题不能为空")
    private String title;
    @NotNull(message = "总分不能为空")
    private Integer totalScore;
    private Integer passScore;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ExamQuestionItemDTO> questions;
}
