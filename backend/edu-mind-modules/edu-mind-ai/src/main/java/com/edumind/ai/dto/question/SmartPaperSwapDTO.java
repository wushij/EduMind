package com.edumind.ai.dto.question;

import lombok.Data;

import java.util.Set;

/**
 * 试卷局部调换题目请求 DTO
 */
@Data
public class SmartPaperSwapDTO {
    private Long courseId;
    private Long oldQuestionId;
    private String type;
    private Integer difficulty;
    private Integer score;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Set<Long> excludeQuestionIds;
    private String promptDirective;
}
