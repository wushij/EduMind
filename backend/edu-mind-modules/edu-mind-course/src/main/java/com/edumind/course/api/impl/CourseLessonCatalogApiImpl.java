package com.edumind.course.api.impl;

import com.edumind.course.api.CourseLessonCatalogApi;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.vo.lesson.PublishedLessonRefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseLessonCatalogApiImpl implements CourseLessonCatalogApi {

    private final ChapterDao chapterDao;

    @Override
    public List<PublishedLessonRefVO> listPublishedLessonChapters(Long courseId) {
        return chapterDao.findPublishedLessonChapters(courseId).stream()
                .map(this::toRef)
                .toList();
    }

    private PublishedLessonRefVO toRef(ChapterEntity lesson) {
        return new PublishedLessonRefVO(lesson.getCourseId(), lesson.getId());
    }
}
