package com.edumind.ai.vo.usage;

import lombok.Data;

import java.util.List;

@Data
public class PersonalAiUsageVO {

    private Integer todayTokensUsed;
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
