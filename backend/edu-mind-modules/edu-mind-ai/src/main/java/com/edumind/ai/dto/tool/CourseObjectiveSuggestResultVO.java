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
public class CourseObjectiveSuggestResultVO {

    private List<CourseObjectiveSuggestItemVO> objectives;

    /** true=真实模型推演成功；false=网络/解析失败后的上下文兜底 */
    private boolean aiGenerated;

    private String sourceLabel;
}
