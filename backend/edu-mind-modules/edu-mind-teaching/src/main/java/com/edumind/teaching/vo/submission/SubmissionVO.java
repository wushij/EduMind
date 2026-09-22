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
    private String studentName;
    /** 学生头像 URL（来自用户档案，可能为空：前端需回退到姓名首字） */
    private String studentAvatar;
    private String studentNo;
    private String assignmentTitle;
    private List<SubmissionAnswerVO> answers;
    private List<GradingItemVO> gradingItems;
}
