package com.edumind.course.api.impl;

import com.edumind.course.api.LessonContentCommandApi;
import com.edumind.course.service.lesson.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonContentCommandApiImpl implements LessonContentCommandApi {

    private final LessonService lessonService;

    @Override
    public void saveLessonContentDraft(Long courseId, Long lessonChapterId, String contentJson) {
        lessonService.saveContentDraft(courseId, lessonChapterId, contentJson);
    }
}
