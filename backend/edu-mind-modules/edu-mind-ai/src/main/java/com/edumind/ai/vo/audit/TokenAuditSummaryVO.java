package com.edumind.ai.vo.audit;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TokenAuditSummaryVO {

    private Integer totalPromptTokens;
    private Integer totalCompletionTokens;
    private Integer totalCalls;
    private Integer avgLatencyMs;
    private String groupBy;
    private List<Map<String, Object>> dailyTrend;
    private List<Map<String, Object>> modelDistribution;
}
