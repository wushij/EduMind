package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiUsageAnalyticsVO {
    private Long totalCalls;
    private Long totalTokens;
    private Long avgLatencyMs = 0L;
    private Long todayCalls = 0L;
    private Long todayTokens = 0L;
    private Double successRate = 99.8;
    private Double totalSavedHours = 0.0;
    private List<DailyUsageVO> daily = new ArrayList<>();
    private List<ProviderUsageVO> byProvider = new ArrayList<>();

    /** 按教育业务场景聚合的真实调用分布（由 ai_call_log.scene 归并得出） */
    private List<SceneUsageVO> byScene = new ArrayList<>();

    @Data
    public static class DailyUsageVO {
        private String date;
        private Long calls;
        private Long tokens;
    }

    @Data
    public static class SceneUsageVO {
        /** 教育业务场景名（如"智能答疑解惑"） */
        private String scene;
        /** 该场景真实调用次数 */
        private Long calls;
        /** 占全部调用的百分比（0~100，保留一位小数） */
        private Double ratio;
        /** 归入该场景的原始 scene 码，便于排查口径 */
        private List<String> sourceScenes = new ArrayList<>();
    }

    @Data
    public static class ProviderUsageVO {
        private String provider;
        private Long calls;
        private Long tokens;
    }
}
