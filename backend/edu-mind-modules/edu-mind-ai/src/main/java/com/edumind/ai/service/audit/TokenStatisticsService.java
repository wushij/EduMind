package com.edumind.ai.service.audit;

import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.audit.TokenAuditSummaryVO;
import com.edumind.common.api.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TokenStatisticsService {

    PageResult<AiCallLogVO> listLogs(Long userId, String scene, String model, String keyword,
                                     LocalDate startDate, LocalDate endDate,
                                     long page, long pageSize);

    TokenAuditSummaryVO summary(String groupBy, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> dailyTrend(int days, LocalDate endDate);
}
