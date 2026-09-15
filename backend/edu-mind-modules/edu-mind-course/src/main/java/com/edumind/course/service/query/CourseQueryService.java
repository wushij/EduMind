package com.edumind.course.service.query;

import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;

import java.util.List;

public interface CourseQueryService {

    CourseDetailVO getCourseById(Long courseId);

    List<CourseVO> listCoursesByIds(List<Long> courseIds);

    List<ChapterTreeVO> listChaptersByCourseId(Long courseId);

    long countCourses();

    List<KnowledgePointVO> listKnowledgePointsByCourseId(Long courseId);

    KnowledgePointVO getKnowledgePointById(Long id);

    List<CourseBriefVO> listRecentCourses(int limit);

    boolean isCourseMember(Long courseId, Long userId);

    List<Long> listCourseIdsByUserId(Long userId);

    List<Long> listStudentUserIdsByCourseId(Long courseId);
}
