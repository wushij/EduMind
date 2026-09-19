package com.edumind.course.api;

import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;

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

    /**
     * 用户是否为课程成员（含教师与学生）
     */
    boolean isCourseMember(Long courseId, Long userId);

    /**
     * 用户可访问的课程 ID 列表（选课成员）
     */
    List<Long> listCourseIdsByUserId(Long userId);

    /**
     * 课程下学生成员 userId 列表（Beta 干预触达受众）
     */
    List<Long> listStudentUserIdsByCourseId(Long courseId);

    /**
     * 指定学生在某课程的课时完成进度摘要
     */
    CourseLessonProgressSummaryVO getLessonProgressSummary(Long courseId, Long studentId);
}
