package com.edumind.course.service.overview;

import com.edumind.course.dto.overview.CourseObjectivesSaveDTO;
import com.edumind.course.vo.overview.CourseObjectiveVO;

import java.util.List;

public interface CourseObjectiveService {
    List<CourseObjectiveVO> list(Long courseId);

    void saveAll(Long courseId, CourseObjectivesSaveDTO dto);
}
