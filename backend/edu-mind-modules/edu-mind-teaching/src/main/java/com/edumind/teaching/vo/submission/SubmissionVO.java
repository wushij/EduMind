package com.edumind.teaching.vo.submission;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SubmissionVO {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String status;
    private Integer totalScore;
    private Integer maxScore;
    private LocalDateTime submitTime;
    private List<SubmissionAnswerVO> answers;
    private List<GradingItemVO> gradingItems;
}
