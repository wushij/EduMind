package com.edumind.statistics.dto.analytics;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 调用审计日志查询 DTO
 */
@Data
public class AiUsageLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 课程 ID，为 null 时表示全部课程 */
    private Long courseId;

    /** 时间范围，如 24h, 7d, 30d, semester */
    private String range = "7d";

    /** 场景筛选码 */
    private String scene;

    /** 模型名称关键字筛选 */
    private String model;

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10 */
    private Integer pageSize = 10;
}
