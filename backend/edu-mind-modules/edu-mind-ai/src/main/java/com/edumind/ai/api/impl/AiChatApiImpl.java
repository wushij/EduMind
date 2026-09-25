package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiChatApi;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.audit.AiCallAuditContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatApiImpl implements AiChatApi {

    private final AiGatewayFacade aiGatewayFacade;

    @Override
    public String chat(String scene, String systemPrompt, String userPrompt) {
        return aiGatewayFacade.chat(scene, systemPrompt, userPrompt);
    }

    /**
     * 课程级调用必须显式携带 courseId：审计落库时 course_id 为空会被课程维度的流水视图过滤掉，
     * 表现为「明明在这门课里点了 AI 诊断，消耗明细里却查不到」。userId / tenantId 仍由审计服务
     * 从当前线程上下文补全，这里只负责把调用方才知道的课程归属传下去。
     */
    @Override
    public String chat(String scene, Long courseId, String systemPrompt, String userPrompt) {
        if (courseId == null) {
            return chat(scene, systemPrompt, userPrompt);
        }
        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .courseId(courseId)
                .build();
        return aiGatewayFacade.chat(scene, null, systemPrompt, userPrompt, auditContext);
    }
}
