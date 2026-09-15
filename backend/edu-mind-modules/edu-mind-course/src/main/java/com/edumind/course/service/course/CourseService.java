package com.edumind.course.service.course;

import com.edumind.common.api.PageResult;
import com.edumind.course.dto.course.CourseCreateDTO;
import com.edumind.course.dto.course.CourseQueryDTO;
import com.edumind.course.dto.course.CourseUpdateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;

import java.util.List;

public interface CourseService {

    PageResult<CourseVO> listCourses(CourseQueryDTO query);

    CourseDetailVO getCourseById(Long id);

    Long createCourse(CourseCreateDTO dto);

    void updateCourse(Long id, CourseUpdateDTO dto);

    void deleteCourse(Long id);

    List<KnowledgePointVO> listKnowledgePoints(Long courseId, Long chapterId);

    KnowledgePointVO createKnowledgePoint(Long courseId, KnowledgePointCreateDTO dto);

    void deleteKnowledgePoint(Long courseId, Long kpId);

    Long joinCourseByCode(String code);
}
