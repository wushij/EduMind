package com.edumind.statistics.dto.learning;

import lombok.Data;

@Data
public class AiPracticeStartDTO {
    private Long courseId;
    private Integer count;
    /** WEAK_POINT | KNOWLEDGE_TIER | ADAPTIVE_SPRINT | WRONG_BATCH | VARIANT | SINGLE_VARIANT */
    private String mode;
    /** 0 或 null 表示全课程薄弱考点自适应 */
    private Long knowledgePointId;
    /** ALL | UNDERSTAND | APPLY | EVALUATE */
    private String cognitiveLevel;
    private Boolean instantFeedback;
    /** 错题本变式 / 单题入口指定的题目 ID */
    private java.util.List<Long> seedQuestionIds;
}
