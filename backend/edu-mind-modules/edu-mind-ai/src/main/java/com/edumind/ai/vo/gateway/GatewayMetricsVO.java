package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GatewayMetricsVO {
    private Long totalRequests;
    private Long totalTokens;
    private Long promptTokens;
    private Long completionTokens;
    private Double estimatedCost;
    private Double successRate;
    private Long avgLatencyMs;
    private Long p95LatencyMs;
    private Long p99LatencyMs;
    private Long fallbackCount;
    private Long circuitOpenCount;
    private Long rateLimitedCount;
    private Long retryCount;

    private List<ProviderMetricVO> byProvider = new ArrayList<>();
    private List<SceneMetricVO> byScene = new ArrayList<>();
    private List<TimeSeriesPointVO> timeSeriesTrend = new ArrayList<>();
    private List<CircuitStateVO> circuitStates = new ArrayList<>();

    @Data
    public static class ProviderMetricVO {
        private String provider;
        /** 命中的模型配置路由键（为空表示历史日志未记录，前端回退展示 provider） */
        private String modelKey;
        private Long calls;
        private Long tokens;
        private Long promptTokens;
        private Long completionTokens;
        private Long avgLatencyMs;
        private Double successRate;
        private Double cost;
    }

    @Data
    public static class SceneMetricVO {
        private String scene;
        private String sceneName;
        private Long calls;
        private Long tokens;
        private Long avgLatencyMs;
    }

    @Data
    public static class TimeSeriesPointVO {
        private String time;
        private Long calls;
        private Long tokens;
        private Long avgLatencyMs;
    }

    @Data
    public static class CircuitStateVO {
        private String modelKey;
        private String status; // CLOSED, OPEN, HALF_OPEN
        private Long openUntilMs;
        private Integer consecutiveFailures;
    }
}
