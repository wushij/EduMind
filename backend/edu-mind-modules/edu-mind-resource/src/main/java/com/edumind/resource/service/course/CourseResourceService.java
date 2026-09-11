package com.edumind.resource.service.course;

import com.edumind.resource.vo.CourseResourceVO;

import java.util.List;

public interface CourseResourceService {

    List<CourseResourceVO> listByCourseId(Long courseId);
}
