package com.edumind.teaching.dto.assignment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentCreateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private Long examId;
    @NotBlank(message = "作业标题不能为空")
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd[ 'T']HH:mm:ss")
    private LocalDateTime deadline;
    private Integer totalScore;
    private Integer passScore;
    private List<Long> questionIds;
    private AssignmentSettingsDTO settings;
}
