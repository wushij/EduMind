package com.edumind.ai.service.assistant.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.chat.ChatStreamRegistry;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.routing.CopilotRagPolicy;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.ai.service.teaching.LessonCopilotEnricher;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.knowledge.api.LessonContentIndexApi;
import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalAssistantServiceImpl implements GlobalAssistantService {

    private final IntentDispatchService intentDispatchService;
    private final AiGatewayFacade aiGatewayFacade;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final LessonQueryApi lessonQueryApi;
    private final LessonContentIndexApi lessonContentIndexApi;
    private final LessonCopilotEnricher lessonCopilotEnricher;
    private final ChatStreamRegistry chatStreamRegistry;
    private final java.util.concurrent.ExecutorService assistantExecutor = java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    public SseEmitter streamChat(GlobalAssistantRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(180000L);
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);

        String userQuestion = dto.getMessage() != null ? dto.getMessage() : "";
        String dispatchMessage = buildDispatchMessage(dto);
        IntentRouter.IntentResult intent = intentDispatchService.route(
                isLessonLearnWithIndex(dto) ? userQuestion : dispatchMessage, dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(buildDispatchRequest(dto, dispatchMessage, intent));

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
            String streamId = chatStreamRegistry.register();
            try {
                sendEvent(emitter, "stream", Map.of("streamId", streamId));

                Map<String, Object> intentData = new HashMap<>();
                intentData.put("route", plan.getRoute());
                intentData.put("agentCode", plan.getAgentCode());
                intentData.put("confidence", intent.confidence());
                intentData.put("slots", intent.slots());
                sendEvent(emitter, "intent", intentData);

                if ("navigate".equals(plan.getRoute()) && plan.getNavigatePayload() != null) {
                    sendEvent(emitter, "navigate", plan.getNavigatePayload());
                }

                if (plan.isUseRag()) {
                    String module = dto.getContextModule() != null ? dto.getContextModule().trim() : "";
                    sendEvent(emitter, "status", Map.of(
                            "phase", "rag_searching",
                            "message", isLessonTeachingContext(module) ? "正在检索本课讲义…" : "正在检索课程资料…"
                    ));
                }

                List<CitationVO> citations = plan.getCitations() != null ? plan.getCitations() : List.of();
                if (!citations.isEmpty()) {
                    sendEvent(emitter, "citation", Map.of("citations", citations));
                }

                StringBuilder contentBuilder = new StringBuilder();
                StringBuilder reasoningBuilder = new StringBuilder();

                LlmClient.StreamCallback relay = LlmStreamRelay.create(
                        (eventName, payload) -> sendEvent(emitter, eventName, payload),
                        contentBuilder,
                        reasoningBuilder);
                aiGatewayFacade.streamChat(
                        "global_assistant",
                        null,
                        plan.getSystemPrompt(),
                        List.of(LlmChatMessage.user(plan.getUserPrompt())),
                        auditContext,
                        () -> chatStreamRegistry.isCancelled(streamId),
                        new LlmClient.StreamCallback() {
                            @Override
                            public void onReasoning(String chunk) {
                                if (chatStreamRegistry.isCancelled(streamId)) {
                                    return;
                                }
                                relay.onReasoning(chunk);
                            }

                            @Override
                            public void onChunk(String chunk) {
                                if (chatStreamRegistry.isCancelled(streamId)) {
                                    return;
                                }
                                relay.onChunk(chunk);
                            }

                            @Override
                            public void onStatus(String phase, String message) {
                                relay.onStatus(phase, message);
                            }

                            @Override
                            public void onComplete() {
                                if (chatStreamRegistry.isCancelled(streamId)) {
                                    emitter.complete();
                                    chatStreamRegistry.remove(streamId);
                                    return;
                                }
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
                                chatStreamRegistry.remove(streamId);
                            }

                            @Override
                            public void onError(String error) {
                                log.error("Global assistant stream error: {}", error);
                                relay.onError(error);
                                emitter.completeWithError(new RuntimeException(error));
                                chatStreamRegistry.remove(streamId);
                            }
                        });
            } catch (Exception ex) {
                log.error("Global assistant error", ex);
                sendEvent(emitter, "error", Map.of("message", ex.getMessage()));
                emitter.completeWithError(ex);
                chatStreamRegistry.remove(streamId);
            }
        }));

        return emitter;
    }

    @Override
    public Map<String, Object> ask(GlobalAssistantRequestDTO dto) {
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);
        String userQuestion = dto.getMessage() != null ? dto.getMessage() : "";
        String dispatchMessage = buildDispatchMessage(dto);
        IntentRouter.IntentResult intent = intentDispatchService.route(
                isLessonLearnWithIndex(dto) ? userQuestion : dispatchMessage, dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(buildDispatchRequest(dto, dispatchMessage, intent));

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
        String module = dto.getContextModule() != null ? dto.getContextModule().trim() : "";
        if (isLessonTeachingContext(module)) {
            boolean learn = "lesson_learn".equalsIgnoreCase(module);
            if (learn && isLessonLearnWithIndex(dto)) {
                return base;
            }
            LessonCopilotContextVO lessonCtx = loadLessonCopilotContext(dto, learn, base);
            StringBuilder sb = new StringBuilder();
            sb.append(learn ? "[课节学习上下文]" : "[课节备课上下文]");
            sb.append("\n说明: 以下为本课讲义内容（课程课节），不是知识库文档。");
            if (dto.getLessonChapterId() != null) {
                sb.append("\n微课节ID: ").append(dto.getLessonChapterId());
            }
            if (dto.getCourseId() != null) {
                sb.append("\n课程ID: ").append(dto.getCourseId());
            }
            String title = lessonCtx != null && StringUtils.hasText(lessonCtx.getTitle())
                    ? lessonCtx.getTitle()
                    : dto.getDraftTitle();
            if (StringUtils.hasText(title)) {
                sb.append("\n课节标题: ").append(title.trim());
            }
            String description = lessonCtx != null && StringUtils.hasText(lessonCtx.getDescription())
                    ? lessonCtx.getDescription()
                    : dto.getDraftDescription();
            if (StringUtils.hasText(description)) {
                sb.append("\n课节导读: ").append(truncate(description, 600));
            }
            String objectives = lessonCtx != null && StringUtils.hasText(lessonCtx.getObjectivesText())
                    ? lessonCtx.getObjectivesText()
                    : dto.getObjectiveExcerpt();
            if (StringUtils.hasText(objectives)) {
                sb.append("\n\n【学习目标】\n").append(objectives.trim());
            }
            String body = lessonCtx != null && StringUtils.hasText(lessonCtx.getRelevantBodyMarkdown())
                    ? lessonCtx.getRelevantBodyMarkdown()
                    : dto.getDraftExcerpt();
            if (StringUtils.hasText(body)) {
                sb.append("\n\n【本课讲义】\n").append(body.trim());
            }
            if (!learn && StringUtils.hasText(dto.getSelectedText())) {
                sb.append("\n\n【编辑器选区】\n").append(truncate(dto.getSelectedText(), 1500));
            }
            sb.append("\n\n").append(learn ? "学生提问: " : "教师提问: ").append(base);
            return sb.toString();
        }
        if (isQuestionBankContext(module)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[题库题目辅导上下文]");
            sb.append("\n说明: 用户正在针对下方完整试题进行学习或备课辅导。请结合题干、选项、参考答案与解析讲解思路；");
            sb.append("若用户未明确要求对答案，优先引导思考而非直接报答案。");
            if (dto.getQuestionId() != null) {
                sb.append("\n题目ID: ").append(dto.getQuestionId());
            }
            if (dto.getCourseId() != null) {
                sb.append("\n课程ID: ").append(dto.getCourseId());
            }
            String stemHint = StringUtils.hasText(dto.getDraftTitle()) ? dto.getDraftTitle() : dto.getDraftDescription();
            if (StringUtils.hasText(stemHint)) {
                sb.append("\n题干摘要: ").append(truncate(stemHint, 200));
            }
            if (StringUtils.hasText(dto.getDraftExcerpt())) {
                sb.append("\n\n【完整题目信息】\n").append(dto.getDraftExcerpt().trim());
            }
            sb.append("\n\n用户提问: ").append(base);
            return sb.toString();
        }
        return base;
    }

    private boolean isQuestionBankContext(String module) {
        return "question_bank".equalsIgnoreCase(module);
    }

    private boolean isLessonTeachingContext(String module) {
        return "lesson_studio".equalsIgnoreCase(module) || "lesson_learn".equalsIgnoreCase(module);
    }

    private LessonCopilotContextVO loadLessonCopilotContext(GlobalAssistantRequestDTO dto, boolean learn,
                                                            String userQuestion) {
        if (dto.getCourseId() == null || dto.getLessonChapterId() == null) {
            return null;
        }
        try {
            return lessonQueryApi.getCopilotContext(
                    dto.getCourseId(),
                    dto.getLessonChapterId(),
                    !learn,
                    userQuestion);
        } catch (Exception ex) {
            log.warn("Load lesson copilot context failed courseId={} lessonId={}: {}",
                    dto.getCourseId(), dto.getLessonChapterId(), ex.getMessage());
            return null;
        }
    }

    private boolean isLessonLearnWithIndex(GlobalAssistantRequestDTO dto) {
        if (!"lesson_learn".equalsIgnoreCase(
                dto.getContextModule() != null ? dto.getContextModule().trim() : "")) {
            return false;
        }
        if (dto.getCourseId() == null || dto.getLessonChapterId() == null) {
            return false;
        }
        return lessonContentIndexApi.findLessonDocumentId(dto.getCourseId(), dto.getLessonChapterId()).isPresent();
    }

    private IntentDispatchRequest buildDispatchRequest(
            GlobalAssistantRequestDTO dto, String dispatchMessage, IntentRouter.IntentResult intent) {
        String module = dto.getContextModule() != null ? dto.getContextModule().trim() : "";
        String userQuestion = dto.getMessage() != null ? dto.getMessage() : "";
        Long knowledgeBaseId = resolveKnowledgeBaseId(dto);
        boolean lessonContext = isLessonTeachingContext(module);
        boolean lessonLearnIndexed = isLessonLearnWithIndex(dto);
        boolean skipRag = CopilotRagPolicy.shouldSkipRag(userQuestion);
        boolean useRag = knowledgeBaseId != null && !skipRag
                && (!lessonContext || lessonLearnIndexed);
        Optional<Long> lessonDocId = Optional.empty();
        String enrichment = "";
        if (lessonContext && dto.getCourseId() != null && dto.getLessonChapterId() != null) {
            boolean previewDraft = !"lesson_learn".equalsIgnoreCase(module);
            enrichment = lessonCopilotEnricher.buildEnrichmentBlock(
                    dto.getCourseId(), dto.getLessonChapterId(), previewDraft, userQuestion);
            lessonDocId = lessonContentIndexApi.findLessonDocumentId(dto.getCourseId(), dto.getLessonChapterId());
        }
        String userPrompt = lessonLearnIndexed ? userQuestion : dispatchMessage;
        return IntentDispatchRequest.builder()
                .message(userPrompt)
                .retrievalQuery(buildRetrievalQuery(dto))
                .courseId(dto.getCourseId())
                .contextModule(module)
                .lessonChapterId(dto.getLessonChapterId())
                .lessonDocumentId(lessonDocId.orElse(null))
                .lessonEnrichmentBlock(enrichment)
                .knowledgeBaseId(useRag ? knowledgeBaseId : null)
                .intent(intent)
                .build();
    }

    private Long resolveKnowledgeBaseId(GlobalAssistantRequestDTO dto) {
        if (dto.getCourseId() == null) {
            return null;
        }
        List<KnowledgeBaseVO> knowledgeBases = knowledgeQueryApi.listKnowledgeBasesByCourseId(dto.getCourseId());
        return knowledgeBases.isEmpty() ? null : knowledgeBases.get(0).getId();
    }

    private String buildRetrievalQuery(GlobalAssistantRequestDTO dto) {
        String question = dto.getMessage() != null ? dto.getMessage().trim() : "";
        if (StringUtils.hasText(dto.getDraftTitle())) {
            return dto.getDraftTitle().trim() + " " + question;
        }
        return question;
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
