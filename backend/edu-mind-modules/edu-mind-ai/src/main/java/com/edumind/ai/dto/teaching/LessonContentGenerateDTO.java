package com.edumind.ai.dto.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonContentGenerateDTO {

    @NotNull
    private Long courseId;

    @NotNull
    private Long lessonChapterId;

    private String depth;
    private String language;
}
