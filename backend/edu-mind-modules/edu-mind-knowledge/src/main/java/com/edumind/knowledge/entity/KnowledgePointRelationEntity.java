package com.edumind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_point_relation")
public class KnowledgePointRelationEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceKnowledgePointId;
    private Long targetKnowledgePointId;
    private String relationType;
    private String properties;
    private LocalDateTime createTime;
}
