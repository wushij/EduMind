package com.edumind.course.api.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.service.query.CourseQueryService;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;
import com.edumind.course.service.lesson.LessonService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseQueryApiImpl implements CourseQueryApi {

    private final CourseQueryService courseQueryService;
    private final LessonService lessonService;

    public CourseQueryApiImpl(CourseQueryService courseQueryService, @Lazy LessonService lessonService) {
        this.courseQueryService = courseQueryService;
        this.lessonService = lessonService;
    }

    @Override
    public CourseDetailVO getCourseById(Long courseId) {
        return courseQueryService.getCourseById(courseId);
    }

    @Override
    public List<CourseVO> listCoursesByIds(List<Long> courseIds) {
        return courseQueryService.listCoursesByIds(courseIds);
    }

    @Override
    public List<ChapterTreeVO> listChaptersByCourseId(Long courseId) {
        return courseQueryService.listChaptersByCourseId(courseId);
    }

    @Override
    public long countCourses() {
        return courseQueryService.countCourses();
    }

    @Override
    public List<KnowledgePointVO> listKnowledgePointsByCourseId(Long courseId) {
        return courseQueryService.listKnowledgePointsByCourseId(courseId);
    }

    @Override
    public KnowledgePointVO getKnowledgePointById(Long id) {
        return courseQueryService.getKnowledgePointById(id);
    }

    @Override
    public List<CourseBriefVO> listRecentCourses(int limit) {
        return courseQueryService.listRecentCourses(limit);
    }

    @Override
    public boolean isCourseMember(Long courseId, Long userId) {
        return courseQueryService.isCourseMember(courseId, userId);
    }

    @Override
    public List<Long> listCourseIdsByUserId(Long userId) {
        return courseQueryService.listCourseIdsByUserId(userId);
    }

    @Override
    public List<Long> listStudentUserIdsByCourseId(Long courseId) {
        return courseQueryService.listStudentUserIdsByCourseId(courseId);
    }

    @Override
    public CourseLessonProgressSummaryVO getLessonProgressSummary(Long courseId, Long studentId) {
        return lessonService.getProgressSummaryForStudent(courseId, studentId);
    }
}
