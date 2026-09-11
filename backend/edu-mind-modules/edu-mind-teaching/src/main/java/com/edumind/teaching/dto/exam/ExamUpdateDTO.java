package com.edumind.teaching.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamUpdateDTO {
    private String title;
    private Integer totalScore;
    private Integer passScore;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private List<ExamQuestionItemDTO> questions;
}
