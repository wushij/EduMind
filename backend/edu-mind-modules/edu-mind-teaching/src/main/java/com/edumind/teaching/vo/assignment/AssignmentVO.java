package com.edumind.teaching.vo.assignment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentVO {
    private Long id;
    private Long courseId;
    private Long examId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String status;
    private LocalDateTime createTime;
}
