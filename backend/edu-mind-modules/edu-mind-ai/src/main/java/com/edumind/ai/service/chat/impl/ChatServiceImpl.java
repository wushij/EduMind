package com.edumind.ai.service.chat.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.chat.ChatHistoryBuilder;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.chat.ChatStreamRegistry;
import com.edumind.ai.service.memory.MemoryPromptBuilder;
import com.edumind.ai.service.memory.retrieval.MemoryContextBlock;
import com.edumind.ai.service.memory.retrieval.MemoryRetrievalService;
import com.edumind.ai.service.routing.CopilotRagPolicy;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.ai.service.teaching.LessonCopilotEnricher;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.knowledge.api.LessonContentIndexApi;
import com.edumind.common.context.TenantContext;
import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final AiGatewayFacade aiGatewayFacade;
    private final LlmProperties llmProperties;
    private final AiSessionCacheService aiSessionCacheService;
    private final ChatStreamRegistry chatStreamRegistry;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final ApplicationEventPublisher eventPublisher;
    private final IntentDispatchService intentDispatchService;
    private final MemoryRetrievalService memoryRetrievalService;
    private final MemoryPromptBuilder memoryPromptBuilder;
    private final ChatHistoryBuilder chatHistoryBuilder;
    private final LessonContentIndexApi lessonContentIndexApi;
    private final LessonCopilotEnricher lessonCopilotEnricher;

    @Override
    public SseEmitter streamChat(ChatStreamDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        Long currentTenantId = TenantContext.requireTenantId();

        ConversationEntity conversation = resolveConversation(dto, userId);
        boolean regenerate = Boolean.TRUE.equals(dto.getRegenerate());
        String savedUserMessageId = null;
        if (regenerate) {
            if (!StringUtils.hasText(dto.getConversationId())) {
                throw new BusinessException("重新生成需要指定会话");
            }
            removeLastAssistantMessage(conversation);
        } else {
            MessageEntity userMessage = saveMessage(conversation.getId(), "user", dto.getMessage(), null, null);
            savedUserMessageId = userMessage.getId();
            aiSessionCacheService.trackUserMessage(conversation.getId(), dto.getMessage());
        }

        String streamId = chatStreamRegistry.register();
        SseEmitter emitter = new SseEmitter(llmProperties.getTimeoutMs().longValue());
        sendEvent(emitter, "stream", Map.of("streamId", streamId));
        final boolean regenerateTurn = regenerate;
        final String userMessageIdForDone = savedUserMessageId;
        final LoginUser currentUser = UserContext.get();
        final Long currentUserId = userId;
        CompletableFuture.runAsync(() -> {
            TenantContext.setTenantId(currentTenantId);
            if (currentUser != null) {
                UserContext.set(currentUser);
            } else {
                UserContext.set(LoginUser.builder().id(currentUserId).build());
            }
            try {
                doStream(conversation, dto, emitter, streamId, regenerateTurn, userMessageIdForDone);
            } finally {
                UserContext.clear();
                TenantContext.clear();
            }
        });
        return emitter;
    }

    @Override
    public void cancelStream(String streamId) {
        chatStreamRegistry.cancel(streamId);
    }

    @Override
    public String buildSystemPromptWithMemory(String baseSystemPrompt, Long courseId, String queryMessage) {
        List<MemoryContextBlock> blocks = memoryRetrievalService.retrieve(courseId, queryMessage, 5);
        return memoryPromptBuilder.buildSystemPromptWithMemory(baseSystemPrompt, blocks);
    }

    private ConversationEntity resolveConversation(ChatStreamDTO dto, Long userId) {
        if (StringUtils.hasText(dto.getConversationId())) {
            ConversationEntity existing = conversationDao.findById(dto.getConversationId());
            if (existing == null || !userId.equals(existing.getUserId())) {
                throw new BusinessException("会话不存在或无权访问");
            }
            return existing;
        }
        ConversationEntity entity = new ConversationEntity();
        Long currentTenantId = TenantContext.requireTenantId();
        entity.setTenantId(currentTenantId);
        entity.setUserId(userId);
        entity.setCourseId(dto.getCourseId());
        entity.setTitle(truncate(dto.getMessage(), 20));
        entity.setMessageCount(0);
        entity.setTotalTokens(0);
        conversationDao.insert(entity);

        AiSessionCacheService.AiSessionState state = new AiSessionCacheService.AiSessionState();
        state.setConversationId(entity.getId());
        state.setUserId(userId);
        state.setCourseId(dto.getCourseId());
        aiSessionCacheService.saveSession(state);
        return entity;
    }

    private void doStream(ConversationEntity conversation, ChatStreamDTO dto, SseEmitter emitter, String streamId,
                          boolean regenerateTurn, String userMessageId) {
        StringBuilder assistantContent = new StringBuilder();
        final Long knowledgeBaseId = resolveKnowledgeBaseId(dto);
        try {
            IntentRouter.IntentResult intent = intentDispatchService.route(dto.getMessage(), dto.getCourseId());
            sendEvent(emitter, "intent", Map.of(
                    "route", intent.type(),
                    "agentCode", intent.targetCode() != null ? intent.targetCode() : "",
                    "confidence", intent.confidence()
            ));

            boolean useRag = shouldUseRag(dto, knowledgeBaseId, intent);
            List<MessageEntity> recentMessages = messageDao.listRecentByConversationId(
                    conversation.getId(), ChatHistoryBuilder.MAX_HISTORY_MESSAGES);
            String conversationHistory = chatHistoryBuilder.formatConversationHistory(recentMessages);

            String userQuestion = dto.getMessage();
            boolean lessonIndexed = dto.getLessonChapterId() != null
                    && dto.getCourseId() != null
                    && lessonContentIndexApi.findLessonDocumentId(dto.getCourseId(), dto.getLessonChapterId())
                    .isPresent();
            String dispatchMessage = userQuestion;
            if (!lessonIndexed) {
                if (dto.getLessonChapterId() != null) {
                    dispatchMessage = "[当前微课节ID: " + dto.getLessonChapterId() + "] " + dispatchMessage;
                } else if (dto.getChapterId() != null) {
                    dispatchMessage = "[当前章节ID: " + dto.getChapterId() + "] " + dispatchMessage;
                }
            }
            Optional<Long> lessonDocId = dto.getLessonChapterId() != null && dto.getCourseId() != null
                    ? lessonContentIndexApi.findLessonDocumentId(dto.getCourseId(), dto.getLessonChapterId())
                    : Optional.empty();
            String enrichment = lessonDocId.isPresent()
                    ? lessonCopilotEnricher.buildEnrichmentBlock(
                    dto.getCourseId(), dto.getLessonChapterId(), false, userQuestion)
                    : "";
            if (lessonIndexed) {
                useRag = knowledgeBaseId != null && !CopilotRagPolicy.shouldSkipRag(userQuestion);
            }
            IntentDispatchRequest dispatchRequest = IntentDispatchRequest.builder()
                    .message(lessonIndexed ? userQuestion : dispatchMessage)
                    .retrievalQuery(userQuestion)
                    .courseId(dto.getCourseId())
                    .contextModule(lessonIndexed ? "lesson_learn" : null)
                    .lessonChapterId(dto.getLessonChapterId())
                    .lessonDocumentId(lessonDocId.orElse(null))
                    .lessonEnrichmentBlock(enrichment)
                    .knowledgeBaseId(useRag ? knowledgeBaseId : null)
                    .documentId(dto.getDocumentId())
                    .intent(intent)
                    .conversationHistory(conversationHistory)
                    .build();
            IntentDispatchPlan plan = intentDispatchService.prepare(dispatchRequest);

            if ("navigate".equals(plan.getRoute()) && plan.getNavigatePayload() != null) {
                sendEvent(emitter, "navigate", plan.getNavigatePayload());
            }

            String systemPrompt = plan.getSystemPrompt();
            String userPrompt = plan.getUserPrompt();
            List<CitationVO> citations = plan.getCitations() != null ? plan.getCitations() : List.of();
            if (!citations.isEmpty()) {
                sendEvent(emitter, "citation", Map.of("citations", citations));
            }

            // 长期记忆检索与个性化上下文注入 (Gate I4 P0-1)
            Long targetCourseId = conversation.getCourseId() != null ? conversation.getCourseId() : dto.getCourseId();
            List<MemoryContextBlock> memoryBlocks = memoryRetrievalService.retrieve(
                    targetCourseId, dto.getMessage(), 5);
            if (memoryBlocks != null && !memoryBlocks.isEmpty()) {
                systemPrompt = memoryPromptBuilder.buildSystemPromptWithMemory(systemPrompt, memoryBlocks);
                sendEvent(emitter, "memory", Map.of("memories", memoryPromptBuilder.toSsePayload(memoryBlocks)));
            }
            systemPrompt = chatHistoryBuilder.appendFollowUpDiscipline(systemPrompt, dto.getMessage());

            List<LlmChatMessage> chatHistory = chatHistoryBuilder.withCurrentUserPrompt(
                    chatHistoryBuilder.build(recentMessages), userPrompt);

            final List<CitationVO> citationsForSave = citations;
            final boolean ragUsed = useRag && knowledgeBaseId != null;
            StringBuilder reasoningContent = new StringBuilder();
            LlmClient.StreamCallback relay = LlmStreamRelay.create(
                    (eventName, payload) -> {
                        if (chatStreamRegistry.isCancelled(streamId)) {
                            return;
                        }
                        sendEvent(emitter, eventName, payload);
                    },
                    assistantContent,
                    reasoningContent
            );
            final String auditScene = ragUsed ? "CHAT_RAG" : "CHAT";
            AiCallAuditContext auditContext = AiCallAuditContext.builder()
                    .userId(LoginUserResolver.resolveUserId())
                    .tenantId(TenantContext.getTenantId())
                    .courseId(conversation.getCourseId())
                    .conversationId(conversation.getId())
                    .knowledgeBaseId(knowledgeBaseId)
                    .retrievalHitCount(citationsForSave != null && !citationsForSave.isEmpty()
                            ? citationsForSave.size() : null)
                    .build();
            aiGatewayFacade.streamChat(auditScene, dto.getModelKey(), systemPrompt, chatHistory, auditContext,
                    new LlmClient.StreamCallback() {
                @Override
                public void onReasoning(String content) {
                    relay.onReasoning(content);
                }

                @Override
                public void onChunk(String content) {
                    if (chatStreamRegistry.isCancelled(streamId)) {
                        return;
                    }
                    relay.onChunk(content);
                    aiSessionCacheService.appendStreamingContent(conversation.getId(), content);
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
                    String citationsJson = citationsForSave.isEmpty()
                            ? null
                            : JSON.toJSONString(citationsForSave);
                    MessageEntity assistantMsg = saveMessage(
                            conversation.getId(),
                            "assistant",
                            assistantContent.toString(),
                            reasoningContent.toString(),
                            citationsJson
                    );
                    updateConversationStats(conversation, regenerateTurn);
                    aiSessionCacheService.markStreamComplete(conversation.getId(), assistantContent.toString());
                    Map<String, String> done = new HashMap<>();
                    done.put("conversationId", conversation.getId());
                    done.put("messageId", assistantMsg.getId());
                    if (StringUtils.hasText(userMessageId)) {
                        done.put("userMessageId", userMessageId);
                    }
                    if (plan.getAgentCode() != null) {
                        done.put("agentCode", plan.getAgentCode());
                    }
                    if (StringUtils.hasText(reasoningContent)) {
                        done.put("reasoningContent", reasoningContent.toString());
                    }
                    sendEvent(emitter, "done", done);
                    emitter.complete();
                    publishChatActivity(conversation);
                    chatStreamRegistry.remove(streamId);
                }

                @Override
                public void onError(String message) {
                    relay.onError(message);
                    aiSessionCacheService.deleteSession(conversation.getId());
                    emitter.completeWithError(new BusinessException(message));
                    chatStreamRegistry.remove(streamId);
                }
            });
        } catch (Exception ex) {
            aiSessionCacheService.deleteSession(conversation.getId());
            sendEvent(emitter, "error", Map.of("code", "AI_TIMEOUT", "message", ex.getMessage()));
            emitter.completeWithError(ex);
            chatStreamRegistry.remove(streamId);
        }
    }

    private MessageEntity saveMessage(String conversationId, String role, String content,
                                      String reasoningContent, String citationsJson) {
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        if (StringUtils.hasText(reasoningContent)) {
            message.setReasoningContent(reasoningContent);
        }
        message.setCitationsJson(citationsJson);
        message.setTokenCount(content != null ? content.length() : 0);
        messageDao.insert(message);
        return message;
    }

    private void removeLastAssistantMessage(ConversationEntity conversation) {
        MessageEntity lastAssistant = messageDao.findLastByConversationIdAndRole(
                conversation.getId(), "assistant");
        if (lastAssistant == null) {
            throw new BusinessException("没有可重新生成的回复");
        }
        messageDao.deleteById(lastAssistant.getId());
        int count = conversation.getMessageCount() != null ? conversation.getMessageCount() : 0;
        conversation.setMessageCount(Math.max(0, count - 1));
        conversationDao.updateById(conversation);
    }

    private void updateConversationStats(ConversationEntity conversation, boolean regenerateTurn) {
        int delta = regenerateTurn ? 1 : 2;
        conversation.setMessageCount(
                (conversation.getMessageCount() != null ? conversation.getMessageCount() : 0) + delta);
        conversationDao.updateById(conversation);
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(JSON.toJSONString(data)));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private Long resolveKnowledgeBaseId(ChatStreamDTO dto) {
        if (dto.getKnowledgeBaseId() != null) {
            return dto.getKnowledgeBaseId();
        }
        if (dto.getCourseId() == null) {
            return null;
        }
        List<KnowledgeBaseVO> knowledgeBases = knowledgeQueryApi.listKnowledgeBasesByCourseId(dto.getCourseId());
        return knowledgeBases.isEmpty() ? null : knowledgeBases.get(0).getId();
    }

    private boolean shouldUseRag(ChatStreamDTO dto, Long knowledgeBaseId, IntentRouter.IntentResult intent) {
        if (dto.getUseRag() != null) {
            return dto.getUseRag();
        }
        if (knowledgeBaseId == null || CopilotRagPolicy.shouldSkipRag(dto.getMessage())) {
            return false;
        }
        if ("rag".equalsIgnoreCase(intent.type())) {
            return true;
        }
        return "chat".equalsIgnoreCase(intent.type());
    }

    private String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "新会话";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private void publishChatActivity(ConversationEntity conversation) {
        if (conversation.getUserId() == null || conversation.getCourseId() == null) {
            return;
        }
        eventPublisher.publishEvent(new LearningActivityEvent(
                this,
                conversation.getUserId(),
                conversation.getCourseId(),
                "AI_CHAT",
                1,
                null));
    }
}
