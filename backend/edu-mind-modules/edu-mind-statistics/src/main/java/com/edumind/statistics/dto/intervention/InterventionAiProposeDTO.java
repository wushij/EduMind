package com.edumind.statistics.dto.intervention;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 教学干预 AI 智能推演请求 DTO
 */
@Data
public class InterventionAiProposeDTO implements Serializable {

    /**
     * 关联课程 ID
     */
    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    /**
     * 指定的薄弱考点 ID (可选，未指定时由 AI 结合学情画像自动巡检最优薄弱点)
     */
    private Long knowledgePointId;

    /**
     * 触发动因类型 (EXAM_WEAK: 考试薄弱断层 / ACTIVITY_DROP: 活跃度异动 / HOMEWORK_DELAY: 作业滞后)
     */
    private String triggerType;
}
