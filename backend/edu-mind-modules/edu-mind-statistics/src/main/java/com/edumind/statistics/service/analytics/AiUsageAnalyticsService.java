package com.edumind.statistics.service.analytics;

import com.edumind.ai.vo.audit.AiCallLogPageVO;
import com.edumind.statistics.dto.analytics.AiUsageLogQueryDTO;
import com.edumind.statistics.vo.analytics.AiUsageAnalyticsVO;

public interface AiUsageAnalyticsService {

    AiUsageAnalyticsVO getUsage(Long courseId, String range);

    AiCallLogPageVO getUsageLogs(AiUsageLogQueryDTO query);
}
