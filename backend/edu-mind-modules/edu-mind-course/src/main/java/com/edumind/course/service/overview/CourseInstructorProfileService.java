package com.edumind.course.service.overview;

import com.edumind.course.dto.overview.CourseInstructorsSaveDTO;
import com.edumind.course.vo.overview.CourseInstructorCardVO;

import java.util.List;

public interface CourseInstructorProfileService {
    List<CourseInstructorCardVO> listCards(Long courseId);

    void saveAll(Long courseId, CourseInstructorsSaveDTO dto);

    void syncFromMembers(Long courseId);
}
