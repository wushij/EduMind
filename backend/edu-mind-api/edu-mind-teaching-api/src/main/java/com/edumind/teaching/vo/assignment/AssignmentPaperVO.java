package com.edumind.teaching.vo.assignment;

import com.edumind.teaching.vo.exam.ExamQuestionVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentPaperVO {
    private Long assignmentId;
    private String title;
    private Long courseId;
    private LocalDateTime deadline;
    private Integer totalScore;
    private Integer passScore;
    private AssignmentSettingsVO settings;
    private List<ExamQuestionVO> questions;
    private String mySubmissionStatus;
    private Long mySubmissionId;
}
