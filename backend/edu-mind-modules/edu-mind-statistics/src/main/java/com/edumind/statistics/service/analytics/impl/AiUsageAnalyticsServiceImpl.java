package com.edumind.statistics.service.analytics.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.ai.vo.audit.AiUsageSummaryVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.statistics.service.analytics.AiUsageAnalyticsService;
import com.edumind.statistics.vo.analytics.AiUsageAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiUsageAnalyticsServiceImpl implements AiUsageAnalyticsService {

    private final AiAuditQueryApi aiAuditQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;

    @Override
    public AiUsageAnalyticsVO getUsage(Long courseId, String range) {
        LocalDateTime since = resolveSince(range);
        List<Long> kbIds = null;
        if (courseId != null) {
            kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                    .map(KnowledgeBaseVO::getId)
                    .collect(Collectors.toList());
        }

        AiUsageSummaryVO summary = aiAuditQueryApi.getUsageSummary(kbIds, since);

        AiUsageAnalyticsVO vo = new AiUsageAnalyticsVO();
        vo.setTotalCalls(summary.getTotalCalls());
        vo.setTotalTokens(summary.getTotalTokens());

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Map.Entry<LocalDate, Long> e : summary.getDailyCalls().entrySet()) {
            AiUsageAnalyticsVO.DailyUsageVO d = new AiUsageAnalyticsVO.DailyUsageVO();
            d.setDate(e.getKey().format(fmt));
            d.setCalls(e.getValue());
            d.setTokens(summary.getDailyTokens().getOrDefault(e.getKey(), 0L));
            vo.getDaily().add(d);
        }
        for (Map.Entry<String, Long> e : summary.getProviderCalls().entrySet()) {
            AiUsageAnalyticsVO.ProviderUsageVO p = new AiUsageAnalyticsVO.ProviderUsageVO();
            p.setProvider(e.getKey());
            p.setCalls(e.getValue());
            p.setTokens(summary.getProviderTokens().getOrDefault(e.getKey(), 0L));
            vo.getByProvider().add(p);
        }
        return vo;
    }

    private LocalDateTime resolveSince(String range) {
        if ("30d".equals(range)) {
            return LocalDateTime.now().minusDays(30);
        }
        if ("7d".equals(range)) {
            return LocalDateTime.now().minusDays(7);
        }
        return LocalDateTime.now().minusDays(1);
    }
}
