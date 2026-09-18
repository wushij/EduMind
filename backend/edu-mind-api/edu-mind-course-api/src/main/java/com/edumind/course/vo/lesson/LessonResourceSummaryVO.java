package com.edumind.course.vo.lesson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResourceSummaryVO implements Serializable {

    private Long id;
    private Long resourceId;
    private String title;
    private String resourceType;
    private String downloadUrl;
}
