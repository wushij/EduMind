package com.edumind.statistics.dto.analytics;

import lombok.Data;

import java.util.List;

@Data
public class TeachingAdviceRequestDTO {
    private Long courseId;
    private List<Long> focusKnowledgePointIds;
}
