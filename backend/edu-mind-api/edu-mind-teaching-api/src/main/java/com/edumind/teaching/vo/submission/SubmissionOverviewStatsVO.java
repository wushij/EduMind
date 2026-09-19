package com.edumind.teaching.vo.submission;

import lombok.Data;

@Data
public class SubmissionOverviewStatsVO {
    private Long total;
    private Long submittedCount;
    private Long gradedCount;
    private Long reviewedCount;
}
