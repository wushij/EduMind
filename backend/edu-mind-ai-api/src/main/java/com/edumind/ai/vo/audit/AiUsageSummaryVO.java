package com.edumind.ai.vo.audit;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 用量审计聚合摘要 VO（跨模块公开模型）
 */
@Data
public class AiUsageSummaryVO {

    private Long totalCalls = 0L;
    private Long totalTokens = 0L;
    private Map<LocalDate, Long> dailyCalls = new HashMap<>();
    private Map<LocalDate, Long> dailyTokens = new HashMap<>();
    private Map<String, Long> providerCalls = new HashMap<>();
    private Map<String, Long> providerTokens = new HashMap<>();
}
