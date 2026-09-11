package com.edumind.teaching.vo.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamVO {
    private Long id;
    private Long courseId;
    private String title;
    private Integer totalScore;
    private Integer passScore;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private List<ExamQuestionVO> questions;
}
