package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.LearningPathVO;

public interface LearningPathService {

    LearningPathVO buildPath(Long courseId);
}
