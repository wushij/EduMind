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
public class CourseKnowledgePointSuggestItemVO {

    private String title;
    private String cognitiveDimension;
    private Integer importance;
    private String description;
    private String examFocus;
    private List<String> prerequisiteTitles;
}
