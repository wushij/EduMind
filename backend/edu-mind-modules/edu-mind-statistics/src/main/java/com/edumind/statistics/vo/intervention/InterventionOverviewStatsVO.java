package com.edumind.statistics.vo.intervention;

import lombok.Data;

import java.io.Serializable;

/**
 * 教学干预决策工作台顶部 KPI 统计概览 VO
 */
@Data
public class InterventionOverviewStatsVO implements Serializable {

    /**
     * 待审核干预提案数
     */
    private Integer pendingCount;

    /**
     * 覆盖预警学生总数
     */
    private Integer totalAffectedStudents;

    /**
     * 干预后掌握度平均提升 (+15.2%)
     */
    private Double avgImprovementRate;

    /**
     * 闭环完成率 (0~100)
     */
    private Double completionRate;

    /**
     * 累计发起干预预案总数
     */
    private Integer totalInterventions;

    /**
     * 已成功下发数
     */
    private Integer dispatchedCount;
}
