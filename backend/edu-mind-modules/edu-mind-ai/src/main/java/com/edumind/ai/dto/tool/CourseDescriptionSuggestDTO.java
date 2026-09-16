package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseDescriptionSuggestDTO {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;
}
