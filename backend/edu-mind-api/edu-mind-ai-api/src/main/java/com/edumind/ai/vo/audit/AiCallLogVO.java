package com.edumind.ai.vo.audit;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 调用与审计日志明细 VO
 */
@Data
public class AiCallLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String userRole;
    private Long courseId;
    private String courseName;
    private String conversationId;
    private String model;
    /** 命中的模型配置路由键（与上游型号区分：同型号可对应多条配置） */
    private String modelKey;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String scene;
    private String sceneLabel;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Integer retrievalHitCount;
    private String citationDocIds;
    private LocalDateTime createTime;
}
