package com.edumind.course.vo.chapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonMetaVO implements Serializable {

    private Integer durationMinutes;
    private String lessonType;
    private String contentStatus;
    private Integer knowledgePointCount;
    private Boolean completed;
    private Boolean hasContent;
}
