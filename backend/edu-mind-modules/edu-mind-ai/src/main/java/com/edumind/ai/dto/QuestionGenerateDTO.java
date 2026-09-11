package com.edumind.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionGenerateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private List<Long> chapterIds;
    private List<Long> knowledgePointIds;
    private List<String> questionTypes;
    private String difficulty;
    @NotNull(message = "出题数量不能为空")
    private Integer count;
    private Integer scorePerQuestion;
}
