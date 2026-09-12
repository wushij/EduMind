package com.edumind.ai.dto.question;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class SmartPaperComposeDTO {
    private Long courseId;
    private List<Long> knowledgePointIds;
    private Integer totalCount = 10;
    private Integer totalScore = 100;
    private Set<Long> excludeIds;
    private Set<Long> excludeQuestionIds;
    private Map<String, Double> typeRatios;
    private Map<String, Double> difficultyDistribution;
    private List<String> cognitiveLevels;
}
