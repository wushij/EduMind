package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.AiUsageAnalyticsVO;

public interface AiUsageAnalyticsService {

    AiUsageAnalyticsVO getUsage(Long courseId, String range);
}
