package com.edumind.ai.vo.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLogVO {

    private Long id;
    private Long userId;
    private String model;
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
