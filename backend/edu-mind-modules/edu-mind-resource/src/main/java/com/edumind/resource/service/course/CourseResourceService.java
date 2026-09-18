package com.edumind.resource.service.course;

import com.edumind.resource.vo.CourseResourceVO;

import java.util.List;

public interface CourseResourceService {

    List<CourseResourceVO> listByCourseId(Long courseId);

    List<CourseResourceVO> listByCourseId(Long courseId, Long chapterId);

    Long addResource(Long courseId, com.edumind.resource.dto.course.CourseResourceCreateDTO dto);

    void deleteResource(Long resourceId);
}
