package com.edumind.ai.vo.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String userRole;
    private Long courseId;
    private String conversationId;
    private String model;
    /** 命中的模型配置路由键（与上游型号区分：同型号可对应多条配置） */
    private String modelKey;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String scene;
    private Long knowledgeBaseId;
    private Integer retrievalHitCount;
    private String citationDocIds;
    private LocalDateTime createTime;
}
