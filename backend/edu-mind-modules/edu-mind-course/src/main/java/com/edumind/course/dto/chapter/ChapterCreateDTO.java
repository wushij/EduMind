package com.edumind.course.dto.chapter;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChapterCreateDTO implements Serializable {

    private String title;
    private Long parentId;
    private Integer sortOrder;
    private String description;
    private Integer durationMinutes;
    private String lessonType;
}
