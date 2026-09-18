package com.edumind.ai.service.assistant.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalAssistantServiceImpl implements GlobalAssistantService {

    private final IntentDispatchService intentDispatchService;
    private final AiGatewayFacade aiGatewayFacade;
    private final java.util.concurrent.ExecutorService assistantExecutor = java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    public SseEmitter streamChat(GlobalAssistantRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(180000L);
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);

        String dispatchMessage = buildDispatchMessage(dto);
        IntentRouter.IntentResult intent = intentDispatchService.route(dispatchMessage, dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(IntentDispatchRequest.builder()
                .message(dispatchMessage)
                .courseId(dto.getCourseId())
                .intent(intent)
                .build());

        final Long userId = LoginUserResolver.requireUserId();
        final Long tenantId = TenantContext.getTenantId();
        final LoginUser currentUser = UserContext.get();
        final AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(userId)
                .tenantId(tenantId)
                .courseId(dto.getCourseId())
                .conversationId(convId)
                .build();

        assistantExecutor.execute(() -> runWithContext(userId, tenantId, currentUser, () -> {
            try {
                Map<String, Object> intentData = new HashMap<>();
                intentData.put("route", plan.getRoute());
                intentData.put("agentCode", plan.getAgentCode());
                intentData.put("confidence", intent.confidence());
                intentData.put("slots", intent.slots());
                sendEvent(emitter, "intent", intentData);

                if ("navigate".equals(plan.getRoute()) && plan.getNavigatePayload() != null) {
                    sendEvent(emitter, "navigate", plan.getNavigatePayload());
                }

                StringBuilder contentBuilder = new StringBuilder();
                StringBuilder reasoningBuilder = new StringBuilder();

                LlmClient.StreamCallback relay = LlmStreamRelay.create(
                        (eventName, payload) -> sendEvent(emitter, eventName, payload),
                        contentBuilder,
                        reasoningBuilder);
                aiGatewayFacade.streamChat("global_assistant", null, plan.getSystemPrompt(), plan.getUserPrompt(),
                        auditContext, new LlmClient.StreamCallback() {
                            @Override
                            public void onReasoning(String chunk) {
                                relay.onReasoning(chunk);
                            }

                            @Override
                            public void onChunk(String chunk) {
                                relay.onChunk(chunk);
                            }

                            @Override
                            public void onStatus(String phase, String message) {
                                relay.onStatus(phase, message);
                            }

                            @Override
                            public void onComplete() {
                                Map<String, Object> done = new HashMap<>();
                                done.put("conversationId", convId);
                                done.put("citations", plan.getCitations() != null ? plan.getCitations() : List.of());
                                if (!reasoningBuilder.isEmpty()) {
                                    done.put("reasoningContent", reasoningBuilder.toString());
                                }
                                if (plan.getAgentCode() != null) {
                                    done.put("agentCode", plan.getAgentCode());
                                }
                                sendEvent(emitter, "done", done);
                                emitter.complete();
                            }

                            @Override
                            public void onError(String error) {
                                log.error("Global assistant stream error: {}", error);
                                relay.onError(error);
                                emitter.completeWithError(new RuntimeException(error));
                            }
                        });
            } catch (Exception ex) {
                log.error("Global assistant error", ex);
                sendEvent(emitter, "error", Map.of("message", ex.getMessage()));
                emitter.completeWithError(ex);
            }
        }));

        return emitter;
    }

    @Override
    public Map<String, Object> ask(GlobalAssistantRequestDTO dto) {
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);
        String dispatchMessage = buildDispatchMessage(dto);
        IntentRouter.IntentResult intent = intentDispatchService.route(dispatchMessage, dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(IntentDispatchRequest.builder()
                .message(dispatchMessage)
                .courseId(dto.getCourseId())
                .intent(intent)
                .build());

        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(LoginUserResolver.requireUserId())
                .tenantId(TenantContext.getTenantId())
                .courseId(dto.getCourseId())
                .conversationId(convId)
                .build();
        String answer = aiGatewayFacade.chat("global_assistant", null, plan.getSystemPrompt(), plan.getUserPrompt(), auditContext);

        String targetCode = resolveTargetCode(intent, plan);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", convId);
        result.put("intent", plan.getRoute());
        result.put("intentDesc", buildIntentDesc(plan));
        result.put("targetCode", targetCode);
        result.put("content", answer);
        result.put("citations", plan.getCitations() != null ? plan.getCitations() : List.of());
        if (plan.getNavigatePayload() != null) {
            result.put("navigate", plan.getNavigatePayload());
        }
        return result;
    }

    private void runWithContext(Long userId, Long tenantId, LoginUser currentUser, Runnable task) {
        if (tenantId != null && tenantId > 0) {
            TenantContext.setTenantId(tenantId);
        }
        if (currentUser != null) {
            UserContext.set(currentUser);
        } else if (userId != null) {
            UserContext.set(LoginUser.builder().id(userId).build());
        }
        try {
            task.run();
        } finally {
            UserContext.clear();
            TenantContext.clear();
        }
    }

    private String resolveTargetCode(IntentRouter.IntentResult intent, IntentDispatchPlan plan) {
        if ("navigate".equals(plan.getRoute())) {
            return plan.getTargetCode();
        }
        String targetCode = intent.targetCode();
        if ("exam".equals(targetCode)) {
            return "/ai/exam/generate";
        }
        if ("learning".equals(targetCode)) {
            return "/analytics/knowledge-mastery";
        }
        if ("kb_retrieval".equals(targetCode)) {
            return "/knowledge/retrieval";
        }
        return targetCode;
    }

    private String buildIntentDesc(IntentDispatchPlan plan) {
        return switch (plan.getRoute()) {
            case "agent" -> "识别意图：Agent 任务编排（" + plan.getAgentCode() + "）";
            case "rag" -> "识别意图：知识库考点检索";
            case "navigate" -> "识别意图：页面功能直达";
            default -> "识别意图：课程助教答疑";
        };
    }

    private String buildDispatchMessage(GlobalAssistantRequestDTO dto) {
        String base = dto.getMessage() != null ? dto.getMessage() : "";
        if (dto.getLessonChapterId() != null && !"lesson_studio".equalsIgnoreCase(String.valueOf(dto.getContextModule()))) {
            return "[当前微课节ID: " + dto.getLessonChapterId() + "] " + base;
        }
        if (!"lesson_studio".equalsIgnoreCase(String.valueOf(dto.getContextModule()))) {
            return base;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[课节备课上下文]");
        if (dto.getLessonChapterId() != null) {
            sb.append("\n微课节ID: ").append(dto.getLessonChapterId());
        }
        if (dto.getCourseId() != null) {
            sb.append("\n课程ID: ").append(dto.getCourseId());
        }
        if (StringUtils.hasText(dto.getDraftTitle())) {
            sb.append("\n课节标题: ").append(dto.getDraftTitle().trim());
        }
        if (StringUtils.hasText(dto.getDraftDescription())) {
            sb.append("\n课节导读: ").append(truncate(dto.getDraftDescription(), 400));
        }
        if (StringUtils.hasText(dto.getObjectiveExcerpt())) {
            sb.append("\n学习目标摘录: ").append(truncate(dto.getObjectiveExcerpt(), 400));
        }
        if (StringUtils.hasText(dto.getDraftExcerpt())) {
            sb.append("\n正文摘录: ").append(truncate(dto.getDraftExcerpt(), 1200));
        }
        if (StringUtils.hasText(dto.getSelectedText())) {
            sb.append("\n编辑器选区: ").append(truncate(dto.getSelectedText(), 800));
        }
        sb.append("\n\n教师提问: ").append(base);
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "…";
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(JSON.toJSONString(data)));
        } catch (IOException e) {
            log.warn("Failed to send SSE event {}: {}", eventName, e.getMessage());
        }
    }
}
