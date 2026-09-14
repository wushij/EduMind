package com.edumind.ai.service.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCallAuditContext {

    private Long userId;
    private Long tenantId;
    private Long courseId;
    /** 可选追溯字段；ai_call_log 与会话无外键，删除聊天记录不影响 Token 审计 */
    private String conversationId;
    private Long knowledgeBaseId;
    private Integer retrievalHitCount;
}
