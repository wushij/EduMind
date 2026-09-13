package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CourseStatisticsVO {

    private Long id;
    private Long courseId;
    private LocalDate statDate;
    private Integer studentCount;
    private BigDecimal avgScore;
    private BigDecimal masteryAvg;
    private Integer aiCallCount;
    private Integer wrongCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
