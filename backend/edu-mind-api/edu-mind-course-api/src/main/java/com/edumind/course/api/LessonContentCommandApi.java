package com.edumind.course.api;

/**
 * 课节内容写入门面（供 AI 模块生成后落库）。
 */
public interface LessonContentCommandApi {

    void saveLessonContentDraft(Long courseId, Long lessonChapterId, String contentJson);
}
