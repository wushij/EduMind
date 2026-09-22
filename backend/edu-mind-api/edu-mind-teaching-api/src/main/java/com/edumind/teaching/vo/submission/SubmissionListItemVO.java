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
    /** 学生头像 URL（来自用户档案，可能为空：前端需回退到姓名首字） */
    private String studentAvatar;
    private String studentNo;
    private String status;
    private Integer totalScore;
    private Integer maxScore;
    private LocalDateTime submitTime;
}
