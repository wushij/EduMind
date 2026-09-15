package com.edumind.course.api.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.service.query.CourseQueryService;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseQueryApiImpl implements CourseQueryApi {

    private final CourseQueryService courseQueryService;

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
}
