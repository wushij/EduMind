package com.edumind.teaching.vo.exam;

import lombok.Data;

import java.util.List;

@Data
public class ExamPreviewVO {
    private String title;
    private Integer totalScore;
    private Integer calculatedScore;
    private Boolean scoreMatched;
    private Integer durationMinutes;
    private List<ExamQuestionVO> questions;
}
