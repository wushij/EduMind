package com.edumind.statistics.entity.intervention;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教学干预建议决策实体
 */
@Data
@TableName("teaching_intervention")
public class TeachingInterventionEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long courseId;

    /**
     * 触发类型 (EXAM_WEAK: 考试薄弱 / ACTIVITY_DROP: 活跃度骤降)
     */
    private String triggerType;

    /**
     * 建议内容 JSON (包含 title, proposalText, affectedStudentCount 等)
     */
    private String proposalJson;

    /**
     * 状态 (PENDING: 待审批 / APPROVED: 已批准 / DISPATCHED: 已分发推送 / REVOKED: 已撤销)
     */
    private String status;

    /**
     * 审批教师 ID
     */
    private Long approvedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
