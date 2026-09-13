package com.edumind.ai.service.assistant.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final AiCallLogDao aiCallLogDao;
    private final LlmProperties llmProperties;
    private final java.util.concurrent.ExecutorService assistantExecutor = java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    public SseEmitter streamChat(GlobalAssistantRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(180000L);
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);

        IntentRouter.IntentResult intent = intentDispatchService.route(dto.getMessage(), dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(IntentDispatchRequest.builder()
                .message(dto.getMessage())
                .courseId(dto.getCourseId())
                .intent(intent)
                .build());

        assistantExecutor.execute(() -> {
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
                long start = System.currentTimeMillis();

                LlmClient.StreamCallback relay = LlmStreamRelay.create(
                        (eventName, payload) -> sendEvent(emitter, eventName, payload),
                        contentBuilder,
                        reasoningBuilder);
                aiGatewayFacade.streamChat("global_assistant", plan.getSystemPrompt(), plan.getUserPrompt(), new LlmClient.StreamCallback() {
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
                        logAudit(dto.getCourseId(), start, dto.getMessage(), contentBuilder.toString());
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
        });

        return emitter;
    }

    @Override
    public Map<String, Object> ask(GlobalAssistantRequestDTO dto) {
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);
        IntentRouter.IntentResult intent = intentDispatchService.route(dto.getMessage(), dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(IntentDispatchRequest.builder()
                .message(dto.getMessage())
                .courseId(dto.getCourseId())
                .intent(intent)
                .build());

        long start = System.currentTimeMillis();
        String answer = aiGatewayFacade.chat("global_assistant", plan.getSystemPrompt(), plan.getUserPrompt());
        logAudit(dto.getCourseId(), start, dto.getMessage(), answer);

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
            case "agent" -> "🎯 识别意图：Agent 任务编排（" + plan.getAgentCode() + "）";
            case "rag" -> "🔍 识别意图：知识库考点检索";
            case "navigate" -> "🧭 识别意图：页面功能直达";
            default -> "🤖 识别意图：课程助教答疑";
        };
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

    private void logAudit(Long courseId, long start, String prompt, String completion) {
        try {
            AiCallLogEntity logEntity = new AiCallLogEntity();
            logEntity.setUserId(LoginUserResolver.resolveUserId());
            logEntity.setCourseId(courseId);
            logEntity.setModel(llmProperties.getModel());
            logEntity.setScene("GLOBAL_ASSISTANT");
            logEntity.setLatencyMs((int) (System.currentTimeMillis() - start));
            logEntity.setPromptTokens(Math.max(1, prompt.length() / 4));
            logEntity.setCompletionTokens(Math.max(1, completion.length() / 4));
            logEntity.setCreateTime(LocalDateTime.now());
            aiCallLogDao.insert(logEntity);
        } catch (Exception ex) {
            log.warn("Failed to insert ai_call_log: {}", ex.getMessage());
        }
    }
}
