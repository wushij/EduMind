package com.edumind.system.vo.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 操作日志统计概览看板 VO（顶部 Hero 胶囊看板）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOperLogStatsVO implements Serializable {

    /** 累计日志总数 */
    private Long totalCount;

    /** 今日操作日志总数 */
    private Long todayCount;

    /** 成功率(百分比，如 99.5) */
    private Double successRate;

    /** 异常操作总数 */
    private Long errorCount;

    /** 平均耗时(毫秒) */
    private Long avgCostTime;
}
