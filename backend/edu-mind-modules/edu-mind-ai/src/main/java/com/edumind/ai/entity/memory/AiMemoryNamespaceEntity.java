package com.edumind.ai.entity.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_memory_namespace")
public class AiMemoryNamespaceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    private String scope;
    private Integer consentStatus;
    private Integer retentionDays;
    private Integer status;
    private LocalDateTime createTime;
}
