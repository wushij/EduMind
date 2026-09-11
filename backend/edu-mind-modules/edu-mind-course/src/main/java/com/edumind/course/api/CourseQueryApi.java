package com.edumind.course.api;

import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;

import java.util.List;

/**
 * 课程领域跨模块只读查询公开 API
 */
public interface CourseQueryApi {

    CourseDetailVO getCourseById(Long courseId);

    List<CourseVO> listCoursesByIds(List<Long> courseIds);

    List<ChapterTreeVO> listChaptersByCourseId(Long courseId);

    long countCourses();

    List<CourseBriefVO> listRecentCourses(int limit);

    List<KnowledgePointVO> listKnowledgePointsByCourseId(Long courseId);

    KnowledgePointVO getKnowledgePointById(Long id);
}
