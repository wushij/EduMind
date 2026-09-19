package com.edumind.teaching.vo.assignment;

import lombok.Data;

@Data
public class AssignmentStatsVO {
    private Long activeAssignmentCount;
    private Long pendingGradingCount;
    private Long aiGradedCount;
    private Double avgSubmissionRate;
}
