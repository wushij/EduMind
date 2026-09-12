package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_mastery")
public class KnowledgeMasteryEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long knowledgePointId;
    private BigDecimal masteryScore;
    private Integer sampleCount;
    private LocalDateTime lastAssessedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
