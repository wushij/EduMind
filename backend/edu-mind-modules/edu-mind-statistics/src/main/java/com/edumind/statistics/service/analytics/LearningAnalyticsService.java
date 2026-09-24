package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;

public interface LearningAnalyticsService {

    LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId);
    LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    StudentPortraitVO getStudentPortrait(Long courseId, Long studentId, String range);
}
