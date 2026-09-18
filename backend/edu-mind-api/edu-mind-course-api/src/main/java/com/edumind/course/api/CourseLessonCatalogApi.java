package com.edumind.course.api;

import com.edumind.course.vo.lesson.PublishedLessonRefVO;

import java.util.List;

public interface CourseLessonCatalogApi {

    /**
     * @param courseId 为空时返回全部已发布微课节
     */
    List<PublishedLessonRefVO> listPublishedLessonChapters(Long courseId);
}
