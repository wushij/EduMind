package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 组织节点实时教学指标 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOrgNodeStatsVO implements Serializable {
    private Long orgId;
    private String orgName;
    private String orgType;
    /** 在册学生人数 */
    private Integer studentCount;
    /** 教师/班主任人数 */
    private Integer teacherCount;
    /** 平均知识掌握度（0.0 ~ 100.0 分） */
    private Double avgMasteryRate;
    /** AI 作业提交率（0.0 ~ 100.0 %） */
    private Double homeworkSubmissionRate;
    /** 待处理教学干预项数 */
    private Integer pendingInterventions;
}
