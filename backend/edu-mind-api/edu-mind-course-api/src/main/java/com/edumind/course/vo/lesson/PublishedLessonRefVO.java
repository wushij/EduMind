package com.edumind.course.vo.lesson;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishedLessonRefVO {
    private Long courseId;
    private Long lessonChapterId;
}
