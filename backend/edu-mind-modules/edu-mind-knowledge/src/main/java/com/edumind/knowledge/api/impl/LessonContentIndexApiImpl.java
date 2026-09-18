package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.LessonContentIndexApi;
import com.edumind.knowledge.service.lesson.impl.LessonContentIndexServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonContentIndexApiImpl implements LessonContentIndexApi {

    private final LessonContentIndexServiceImpl lessonContentIndexService;

    @Override
    public void ingestLesson(Long courseId, Long lessonChapterId) {
        lessonContentIndexService.ingestLesson(courseId, lessonChapterId);
    }

    @Override
    public void removeLessonIndex(Long courseId, Long lessonChapterId) {
        lessonContentIndexService.removeLessonIndex(courseId, lessonChapterId);
    }

    @Override
    public Optional<Long> findLessonDocumentId(Long courseId, Long lessonChapterId) {
        return lessonContentIndexService.findLessonDocumentId(courseId, lessonChapterId);
    }

    @Override
    public int reindexPublishedLessons(Long courseId) {
        return lessonContentIndexService.reindexPublishedLessons(courseId);
    }
}
