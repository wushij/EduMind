package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.WrongQuestionAnalyticsVO;

public interface WrongQuestionAnalyticsService {

    WrongQuestionAnalyticsVO listWrongQuestions(Long courseId, Long knowledgePointId, int page, int pageSize);
}
