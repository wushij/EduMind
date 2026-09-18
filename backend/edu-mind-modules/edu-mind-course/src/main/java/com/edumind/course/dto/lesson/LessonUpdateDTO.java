package com.edumind.course.dto.lesson;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LessonUpdateDTO implements Serializable {

    private String title;
    private String description;
    private Integer durationMinutes;
    private String lessonType;
    private String contentJson;
    private List<Long> knowledgePointIds;
}
