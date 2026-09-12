package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("course_statistics")
public class CourseStatisticsEntity implements Serializable {

    @TableId(type = IdType.AUTO)
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
