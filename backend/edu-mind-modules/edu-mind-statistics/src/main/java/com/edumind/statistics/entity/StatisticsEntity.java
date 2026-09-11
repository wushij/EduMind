package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 学情与 AI 调用统计快照实体
 */
@Data
@TableName("statistics_daily_snapshot")
public class StatisticsEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long courseId;
    private Integer activeStudentCount;
    private Integer totalAiConversations;
    private Long totalTokensConsumed;
    private Double avgScore;
}
