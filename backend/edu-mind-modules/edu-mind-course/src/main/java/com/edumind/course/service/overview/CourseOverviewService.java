package com.edumind.course.service.overview;

import com.edumind.course.vo.overview.CourseOverviewVO;

public interface CourseOverviewService {
    CourseOverviewVO getOverview(Long courseId);
}
