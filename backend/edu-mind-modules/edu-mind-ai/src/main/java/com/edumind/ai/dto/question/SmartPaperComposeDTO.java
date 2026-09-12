package com.edumind.ai.dto.question;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SmartPaperComposeDTO {
    private Long courseId;
    private List<Long> knowledgePointIds;
    private Integer totalCount = 10;
    private Set<Long> excludeIds;
}
