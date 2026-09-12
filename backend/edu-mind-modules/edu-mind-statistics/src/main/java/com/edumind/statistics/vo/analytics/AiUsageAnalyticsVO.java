package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiUsageAnalyticsVO {
    private Long totalCalls;
    private Long totalTokens;
    private List<DailyUsageVO> daily = new ArrayList<>();
    private List<ProviderUsageVO> byProvider = new ArrayList<>();

    @Data
    public static class DailyUsageVO {
        private String date;
        private Long calls;
        private Long tokens;
    }

    @Data
    public static class ProviderUsageVO {
        private String provider;
        private Long calls;
        private Long tokens;
    }
}
