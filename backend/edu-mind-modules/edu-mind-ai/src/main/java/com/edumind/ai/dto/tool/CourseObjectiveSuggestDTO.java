package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseObjectiveSuggestDTO {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /** 最多生成条数，默认 4，上限 6 */
    private Integer count;
}
