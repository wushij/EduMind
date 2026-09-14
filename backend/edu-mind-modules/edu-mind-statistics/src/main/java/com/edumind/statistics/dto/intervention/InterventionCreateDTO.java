package com.edumind.statistics.dto.intervention;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 教学干预提案创建请求 DTO
 */
@Data
public class InterventionCreateDTO implements Serializable {

    /**
     * 关联课程 ID
     */
    private Long courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 触发类型 (EXAM_WEAK: 考试薄弱 / ACTIVITY_DROP: 活跃度骤降 / HOMEWORK_ABNORMAL: 作业异常)
     */
    private String triggerType;

    /**
     * 干预标题 / 告警摘要
     */
    @NotBlank(message = "干预标题不能为空")
    private String title;

    /**
     * 建议干预方案描述
     */
    @NotBlank(message = "干预方案描述不能为空")
    private String proposalText;

    /**
     * 影响学生人数
     */
    private Integer affectedStudentCount;

    /**
     * 靶向习题 ID 列表
     */
    private List<Long> customQuestionIds;
}
