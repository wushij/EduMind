package com.edumind.course.vo.lesson;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonIndexSourceVO {

    private Long courseId;
    private Long lessonChapterId;
    private String title;
    private String description;
    private String markdown;
    private String contentHash;
    private boolean published;
}
