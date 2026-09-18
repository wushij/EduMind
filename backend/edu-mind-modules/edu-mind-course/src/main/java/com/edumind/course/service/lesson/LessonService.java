package com.edumind.course.service.lesson;

import com.edumind.course.dto.lesson.LessonProgressUpdateDTO;
import com.edumind.course.dto.lesson.LessonUpdateDTO;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;
import com.edumind.course.vo.lesson.LessonDetailVO;
import com.edumind.course.vo.lesson.LessonProgressVO;

public interface LessonService {

    LessonDetailVO getLessonDetail(Long courseId, Long lessonId, boolean previewDraft);

    void updateLesson(Long courseId, Long lessonId, LessonUpdateDTO dto);

    void publishLesson(Long courseId, Long lessonId);

    void unpublishLesson(Long courseId, Long lessonId);

    void saveContentDraft(Long courseId, Long lessonId, String contentJson);

    LessonProgressVO getProgress(Long courseId, Long lessonId);

    LessonProgressVO updateProgress(Long courseId, Long lessonId, LessonProgressUpdateDTO dto);

    CourseLessonProgressSummaryVO getProgressSummary(Long courseId);
}
