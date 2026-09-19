package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.LearningHomeOverviewVO;

public interface LearningHomeService {

    LearningHomeOverviewVO getOverview(Long studentId, Long primaryCourseId);
}
