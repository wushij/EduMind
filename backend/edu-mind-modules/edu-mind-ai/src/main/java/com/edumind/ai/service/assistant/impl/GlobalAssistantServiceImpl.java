package com.edumind.ai.service.assistant.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.gateway.AiUserModelPolicy;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.chat.ChatHistoryBuilder;
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
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final AiUserModelPolicy aiUserModelPolicy;
    /** 会话消息持久化：多轮上下文依赖它按 conversationId 取回历史（原实现完全不带历史，导致「愿意」等追问丢失语境） */
    private final MessageDao messageDao;
    private final ConversationDao conversationDao;
    private final ChatHistoryBuilder chatHistoryBuilder;
    private final java.util.concurrent.ExecutorService assistantExecutor = java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    public SseEmitter streamChat(GlobalAssistantRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(180000L);
        String convId = dto.getConversationId() != null ? dto.getConversationId() : "conv-" + UUID.randomUUID().toString().substring(0, 8);

        String userQuestion = dto.getMessage() != null ? dto.getMessage() : "";
        String dispatchMessage = buildDispatchMessage(dto);
        boolean routeByDirectQuestion = isLessonLearnWithIndex(dto) || isQuestionBankContext(dto.getContextModule());
        IntentRouter.IntentResult intent = intentDispatchService.route(
                routeByDirectQuestion ? userQuestion : dispatchMessage, dto.getCourseId());
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

        // 多轮上下文闭环：先确保会话主记录存在并落库本轮用户提问。
        // 落库必须早于读历史，这样 listRecentByConversationId 取回的末条即本轮提问，
        // 再交由 ChatHistoryBuilder.withCurrentUserPrompt 覆盖为携带课程/课节上下文的完整 prompt。
        // 越权校验失败会直接抛出；其余持久化异常只降级为「本轮无历史」，不阻断对话。
        ensureConversationQuietly(convId, userId, dto);
        saveMessageQuietly(convId, "user", userQuestion, null, null);

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
                        // 与课程问答保持一致：接收并校验前端选择的模型，未通过治理校验时回落场景策略
                        aiUserModelPolicy.validateUserSelection(dto.getModelKey()),
                        chatHistoryBuilder.appendFollowUpDiscipline(plan.getSystemPrompt(), userQuestion),
                        buildChatHistory(convId, plan.getUserPrompt()),
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
                                    // 用户主动中止：本轮内容不完整，不写入历史，避免污染后续多轮上下文
                                    emitter.complete();
                                    chatStreamRegistry.remove(streamId);
                                    return;
                                }
                                // 落库本轮回答，供下一次提问时作为历史上下文取回
                                List<CitationVO> citationsForSave = plan.getCitations() != null
                                        ? plan.getCitations() : List.of();
                                saveMessageQuietly(convId, "assistant", contentBuilder.toString(),
                                        reasoningBuilder.toString(),
                                        citationsForSave.isEmpty() ? null : JSON.toJSONString(citationsForSave));
                                updateConversationStatsQuietly(convId, 2);
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
        boolean routeByDirectQuestion = isLessonLearnWithIndex(dto) || isQuestionBankContext(dto.getContextModule());
        IntentRouter.IntentResult intent = intentDispatchService.route(
                routeByDirectQuestion ? userQuestion : dispatchMessage, dto.getCourseId());
        IntentDispatchPlan plan = intentDispatchService.prepare(buildDispatchRequest(dto, dispatchMessage, intent));

        Long userId = LoginUserResolver.requireUserId();
        ensureConversationQuietly(convId, userId, dto);
        // 前端流式失败后会带着同一 conversationId 回退到本接口，此时本轮提问可能已由流式链路落库，
        // 必须按内容去重，否则历史里会出现连续两条相同的 user，破坏多轮语境
        boolean userTurnSaved = saveUserMessageIfAbsent(convId, userQuestion);

        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(userId)
                .tenantId(TenantContext.getTenantId())
                .courseId(dto.getCourseId())
                .conversationId(convId)
                .build();
        String answer = aiGatewayFacade.chat("global_assistant",
                aiUserModelPolicy.validateUserSelection(dto.getModelKey()),
                chatHistoryBuilder.appendFollowUpDiscipline(plan.getSystemPrompt(), userQuestion),
                buildHistoryBlock(convId, plan.getUserPrompt()), auditContext);

        List<CitationVO> citationsForSave = plan.getCitations() != null ? plan.getCitations() : List.of();
        saveMessageQuietly(convId, "assistant", answer, null,
                citationsForSave.isEmpty() ? null : JSON.toJSONString(citationsForSave));
        // 去重命中时本轮只新增了一条 assistant 消息
        updateConversationStatsQuietly(convId, userTurnSaved ? 2 : 1);

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

    // ==================== 多轮上下文与会话持久化 ====================

    /**
     * 组装多轮上下文。
     *
     * <p>历史末条是本次刚落库的用户提问，{@link ChatHistoryBuilder#withCurrentUserPrompt}
     * 会将其替换为携带课程 / 课节 / 题库上下文的完整 prompt，因此既保留前几轮语境，
     * 又不会让模型看到两条重复的当前提问。</p>
     */
    private List<LlmChatMessage> buildChatHistory(String convId, String currentUserPrompt) {
        try {
            List<MessageEntity> recentMessages = messageDao.listRecentByConversationId(
                    convId, ChatHistoryBuilder.MAX_HISTORY_MESSAGES);
            return chatHistoryBuilder.withCurrentUserPrompt(
                    chatHistoryBuilder.build(recentMessages), currentUserPrompt);
        } catch (Exception ex) {
            log.warn("Load assistant chat history failed convId={}: {}", convId, ex.getMessage());
            return List.of(LlmChatMessage.user(currentUserPrompt));
        }
    }

    /**
     * 非流式回退接口的网关只提供「单串 userPrompt」重载（多轮 messages 重载仅流式链路具备），
     * 因此把历史压缩为文本块前置到当前 prompt，保证 /ai/assistant/ask 同样具备多轮记忆。
     */
    private String buildHistoryBlock(String convId, String currentUserPrompt) {
        try {
            List<MessageEntity> recentMessages = messageDao.listRecentByConversationId(
                    convId, ChatHistoryBuilder.MAX_HISTORY_MESSAGES);
            if (!recentMessages.isEmpty()
                    && "user".equalsIgnoreCase(recentMessages.get(recentMessages.size() - 1).getRole())) {
                // 剔除刚落库的本轮提问，避免与下方「当前提问」重复
                recentMessages = new ArrayList<>(recentMessages.subList(0, recentMessages.size() - 1));
            }
            String history = chatHistoryBuilder.formatConversationHistory(recentMessages);
            if (!StringUtils.hasText(history)) {
                return currentUserPrompt;
            }
            return "【历史对话】\n" + history + "\n\n【当前提问】\n" + currentUserPrompt;
        } catch (Exception ex) {
            log.warn("Load assistant chat history failed convId={}: {}", convId, ex.getMessage());
            return currentUserPrompt;
        }
    }

    /**
     * 确保会话主记录存在：让消息有归属，历史亦可跨端恢复。
     * 会话已属于他人时直接拒绝，防止按 conversationId 猜读他人对话。
     */
    private void ensureConversationQuietly(String convId, Long userId, GlobalAssistantRequestDTO dto) {
        runQuietly(() -> {
            ConversationEntity existing = conversationDao.findById(convId);
            if (existing != null) {
                if (!userId.equals(existing.getUserId())) {
                    throw new BusinessException("会话不存在或无权访问");
                }
                return;
            }
            ConversationEntity entity = new ConversationEntity();
            entity.setId(convId);
            entity.setTenantId(TenantContext.getTenantId());
            entity.setUserId(userId);
            entity.setCourseId(dto.getCourseId());
            entity.setTitle(truncate(dto.getMessage(), 30));
            entity.setMessageCount(0);
            entity.setTotalTokens(0);
            conversationDao.insert(entity);
        });
    }

    private void saveMessageQuietly(String convId, String role, String content,
                                    String reasoningContent, String citationsJson) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        runQuietly(() -> {
            MessageEntity message = new MessageEntity();
            message.setConversationId(convId);
            message.setRole(role);
            message.setContent(content);
            if (StringUtils.hasText(reasoningContent)) {
                message.setReasoningContent(reasoningContent);
            }
            message.setCitationsJson(citationsJson);
            message.setTokenCount(content.length());
            // 显式写入毫秒级时间戳：多轮历史排序完全依赖 create_time（见 V2_6_6 迁移）
            message.setCreateTime(LocalDateTime.now());
            messageDao.insert(message);
        });
    }

    /**
     * 落库本轮用户提问，若末条 user 消息内容相同则跳过。
     *
     * <p>回退场景：流式链路失败 / 超时后，前端会带着同一 conversationId 改调
     * {@code /ai/assistant/ask}，此时提问已被流式链路写入，重复写入会让模型看到
     * 「user, user, assistant」，因此按内容去重。</p>
     *
     * @return 是否真正新增了一条 user 消息
     */
    private boolean saveUserMessageIfAbsent(String convId, String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        try {
            MessageEntity last = messageDao.findLastByConversationIdAndRole(convId, "user");
            if (last != null && content.equals(last.getContent())) {
                return false;
            }
            MessageEntity message = new MessageEntity();
            message.setConversationId(convId);
            message.setRole("user");
            message.setContent(content);
            message.setTokenCount(content.length());
            message.setCreateTime(LocalDateTime.now());
            messageDao.insert(message);
            return true;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Assistant conversation persistence skipped: {}", ex.getMessage());
            return false;
        }
    }

    private void updateConversationStatsQuietly(String convId, int delta) {
        runQuietly(() -> {
            ConversationEntity conversation = conversationDao.findById(convId);
            if (conversation == null) {
                return;
            }
            int count = conversation.getMessageCount() != null ? conversation.getMessageCount() : 0;
            conversation.setMessageCount(count + delta);
            conversationDao.updateById(conversation);
        });
    }

    /**
     * 持久化失败只降级为「本轮无历史」，绝不阻断对话；越权异常必须向上抛出。
     */
    private void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Assistant conversation persistence skipped: {}", ex.getMessage());
        }
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
            case "agent" -> {
                String code = plan.getAgentCode() != null ? plan.getAgentCode().toLowerCase() : "";
                if ("exam".equals(code) || "question".equals(code)) {
                    yield "识别意图：AI 智能组卷与出题";
                }
                if ("tutor".equals(code) || "question_tutor".equals(code)) {
                    yield "识别意图：题目答疑辅导";
                }
                if ("teaching".equals(code)) {
                    yield "识别意图：备课教学建议";
                }
                if ("grading".equals(code)) {
                    yield "识别意图：作业智能批改";
                }
                if ("learning".equals(code)) {
                    yield "识别意图：学情诊断分析";
                }
                yield "识别意图：AI 智能助教协同";
            }
            case "rag" -> "识别意图：知识库考点检索";
            case "navigate" -> "识别意图：页面功能直达";
            default -> {
                String code = plan.getAgentCode() != null ? plan.getAgentCode().toLowerCase() : "";
                if ("tutor".equals(code) || "question_tutor".equals(code)) {
                    yield "识别意图：题目答疑辅导";
                }
                if ("platform".equals(code)) {
                    yield "识别意图：平台能力咨询";
                }
                yield "识别意图：课程助教答疑";
            }
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
                // 前端在全域模式下不传 contextModule，据此判定「未锚定任何上下文」
                .globalScope(!StringUtils.hasText(module))
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
