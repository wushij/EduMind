package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseKnowledgePointSuggestDTO {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Long chapterId;

    /** 生成条数，默认 4，上限 6 */
    private Integer count;
}
