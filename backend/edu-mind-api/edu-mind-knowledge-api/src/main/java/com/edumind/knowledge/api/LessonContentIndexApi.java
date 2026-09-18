package com.edumind.knowledge.api;

import java.util.Optional;

/**
 * 课节讲义发布索引（虚拟文档 + 向量），供 course 模块在发布/下架时调用。
 */
public interface LessonContentIndexApi {

    void ingestLesson(Long courseId, Long lessonChapterId);

    void removeLessonIndex(Long courseId, Long lessonChapterId);

    Optional<Long> findLessonDocumentId(Long courseId, Long lessonChapterId);

    /** 批量重建已发布课节索引（courseId 为空则全库） */
    int reindexPublishedLessons(Long courseId);
}
