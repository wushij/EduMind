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
public class CourseLessonProgressSummaryVO implements Serializable {

    private Integer totalLessonCount;
    private Integer completedLessonCount;
}
