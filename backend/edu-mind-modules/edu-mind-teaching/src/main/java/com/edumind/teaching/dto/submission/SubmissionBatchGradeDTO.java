package com.edumind.teaching.dto.submission;

import lombok.Data;

import java.util.List;

@Data
public class SubmissionBatchGradeDTO {
    private Long courseId;
    private Long assignmentId;
    private List<Long> submissionIds;
    private Boolean forceRegrade;
}
