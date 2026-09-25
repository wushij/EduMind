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
     * 关联薄弱知识点 ID
     */
    private Long knowledgePointId;

    /**
     * 关联薄弱知识点名称
     */
    private String knowledgePointTitle;

    /**
     * 触发类型 (EXAM_WEAK: 考试薄弱 / ACTIVITY_DROP: 活跃度骤降 / HOMEWORK_DELAY: 作业滞后)
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
     * 关联的具体预警学生用户 ID 列表
     */
    private List<Long> targetStudentIds;

    /**
     * 选配的课程微课/课件资源 ID 列表
     */
    private List<Long> resourceIds;

    /**
     * 靶向习题 ID 列表
     */
    private List<Long> customQuestionIds;

    /**
     * 预期掌握度提升预估 (如 "+15% ~ +20%")
     */
    private String expectedImprovement;
}
