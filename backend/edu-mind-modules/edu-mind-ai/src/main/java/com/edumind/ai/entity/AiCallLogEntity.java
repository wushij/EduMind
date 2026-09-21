package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLogEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    private String conversationId;
    private String model;
    /** 命中的模型配置路由键：同一上游型号可能被多条配置复用，用于区分实际走了哪条配置 */
    private String modelKey;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
    private String scene;
    private Long knowledgeBaseId;
    private Integer retrievalHitCount;
    private String citationDocIds;
    private LocalDateTime createTime;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
