package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;

public interface LearningAnalyticsService {

    LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId);

    StudentPortraitVO getStudentPortrait(Long courseId, Long studentId);
}
