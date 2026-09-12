package com.edumind.ai.service.assistant.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.assistant.GlobalAssistantService;
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

    private final IntentRouter intentRouter;
    private final AiGatewayFacade aiGatewayFacade;
    private final AiCallLogDao aiCallLogDao;
    private final LlmProperties llmProperties;
    private final java.util.concurrent.ExecutorService assistantExecutor = java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    public SseEmitter streamChat(GlobalAssistantRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(180000L);
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. 意图分流
        IntentRouter.IntentResult intent = intentRouter.route(dto.getMessage(), dto.getCourseId());

        assistantExecutor.execute(() -> {
            try {
                // 发送 intent 事件
                Map<String, Object> intentData = new HashMap<>();
                intentData.put("route", intent.type());
                intentData.put("agentCode", intent.targetCode());
                intentData.put("confidence", intent.confidence());
                intentData.put("slots", intent.slots());
                sendEvent(emitter, "intent", intentData);

                // 2. 调用大模型流式生成
                StringBuilder contentBuilder = new StringBuilder();
                String systemPrompt = "你是一个全能教学AI助手EduMind，请根据用户的输入专业、友好地回答。当前识别意图：" + intent.type();
                long start = System.currentTimeMillis();

                aiGatewayFacade.streamChat("global_assistant", systemPrompt, dto.getMessage(), new LlmClient.StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        contentBuilder.append(chunk);
                        Map<String, String> delta = new HashMap<>();
                        delta.put("content", chunk);
                        sendEvent(emitter, "delta", delta);
                    }

                    @Override
                    public void onComplete() {
                        Map<String, Object> done = new HashMap<>();
                        done.put("conversationId", convId);
                        done.put("citations", List.of());
                        sendEvent(emitter, "done", done);
                        emitter.complete();

                        // 审计落库
                        logAudit(dto.getCourseId(), start, dto.getMessage(), contentBuilder.toString());
                    }

                    @Override
                    public void onError(String error) {
                        log.error("Global assistant stream error: {}", error);
                        sendEvent(emitter, "error", Map.of("message", error));
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
        IntentRouter.IntentResult intent = intentRouter.route(dto.getMessage(), dto.getCourseId());
        String systemPrompt = "你是一个全能教学AI助手EduMind，请根据用户的输入专业、友好地回答。当前识别意图：" + intent.type();
        long start = System.currentTimeMillis();
        String answer = aiGatewayFacade.chat("global_assistant", systemPrompt, dto.getMessage());

        logAudit(dto.getCourseId(), start, dto.getMessage(), answer);

        String targetCode = intent.targetCode();
        if ("exam".equals(targetCode)) {
            targetCode = "/ai/exam/generate";
        } else if ("learning".equals(targetCode)) {
            targetCode = "/analytics/knowledge-mastery";
        } else if ("kb_retrieval".equals(targetCode)) {
            targetCode = "/knowledge/retrieval";
        }

        String intentDesc;
        if ("agent".equals(intent.type()) && "exam".equals(intent.targetCode())) {
            intentDesc = "🎯 识别意图：AI智能组卷与出题";
        } else if ("rag".equals(intent.type())) {
            intentDesc = "🔍 识别意图：知识库考点检索";
        } else if ("navigate".equals(intent.type())) {
            intentDesc = "🧭 识别意图：页面功能直达";
        } else {
            intentDesc = "🤖 识别意图：课程助教答疑";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", convId);
        result.put("intent", intent.type());
        result.put("intentDesc", intentDesc);
        result.put("targetCode", targetCode);
        result.put("content", answer);
        result.put("citations", List.of());
        return result;
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
