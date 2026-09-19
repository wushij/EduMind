package com.edumind.teaching.vo.submission;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionListItemVO {
    private Long id;
    private Long assignmentId;
    private Long courseId;
    private String courseName;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String status;
    private Integer totalScore;
    private Integer maxScore;
    private LocalDateTime submitTime;
}
