package com.edumind.ai.vo.usage;

import lombok.Data;

import java.util.List;

@Data
public class PersonalAiUsageVO {

    private Integer todayTokensUsed;
    private Integer weekTokensUsed;
    private Integer monthTokensUsed;
    private Integer totalTokensUsed;

    private Integer todayCalls;
    private Integer weekCalls;
    private Integer monthCalls;
    private Integer totalCalls;

    private Double todayCostRMB;
    private Double weekCostRMB;
    private Double monthCostRMB;
    private Double totalCostRMB;

    private Integer dailyTokenLimit;
    private Integer remainingPercent;
    private String quotaStatus;

    private Integer totalQaAndGenerateCalls;
    private Integer avgLatencyMs;

    private Double semesterEstimatedCostRMB;

    private List<PersonalAiUsageLogVO> recentLogs;
    private Long totalLogCount;
    private Long logPageNum;
    private Long logPageSize;
}
