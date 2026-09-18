package com.edumind.course.dto.lesson;

import lombok.Data;

import java.io.Serializable;

@Data
public class LessonProgressUpdateDTO implements Serializable {

    private String status;
    private Integer progressPercent;
    private String lastBlockId;
    private Integer durationMinutes;
}
