package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GatewayMetricsVO {
    private Long totalRequests;
    private Double successRate;
    private Long avgLatencyMs;
    private Long fallbackCount;
    private List<ProviderMetricVO> byProvider = new ArrayList<>();

    @Data
    public static class ProviderMetricVO {
        private String provider;
        private Long calls;
        private Long tokens;
    }
}
