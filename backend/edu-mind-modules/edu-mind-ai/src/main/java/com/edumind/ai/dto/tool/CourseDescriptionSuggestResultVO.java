package com.edumind.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDescriptionSuggestResultVO {

    private String text;
    private boolean aiGenerated;
    private String sourceLabel;
}
