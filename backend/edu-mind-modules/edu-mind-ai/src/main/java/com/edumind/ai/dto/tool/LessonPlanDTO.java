package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonPlanDTO {
    @NotNull
    private Long courseId;
    @NotBlank
    private String topic;
    private Integer hours;
    private String objectives;
}
