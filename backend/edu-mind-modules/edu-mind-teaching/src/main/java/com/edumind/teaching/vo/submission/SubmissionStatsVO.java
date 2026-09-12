package com.edumind.teaching.vo.submission;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubmissionStatsVO {
    private Long courseId;
    private Integer totalAssignments;
    private Integer submittedCount;
    private Integer gradedCount;
    private Double avgSubmissionRate;
    private Double avgScore;
    private List<StudentScoreVO> studentScores = new ArrayList<>();

    @Data
    public static class StudentScoreVO {
        private Long studentId;
        private Double avgScore;
        private Integer submissionCount;
    }
}
