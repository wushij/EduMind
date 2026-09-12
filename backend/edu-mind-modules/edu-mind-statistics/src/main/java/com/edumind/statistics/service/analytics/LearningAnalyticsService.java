package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;

public interface LearningAnalyticsService {

    LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId);
}
