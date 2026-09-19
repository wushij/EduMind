package com.edumind.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseKnowledgePointSuggestResultVO {

    private List<CourseKnowledgePointSuggestItemVO> points;
    private Boolean aiGenerated;
    private String sourceLabel;
}
