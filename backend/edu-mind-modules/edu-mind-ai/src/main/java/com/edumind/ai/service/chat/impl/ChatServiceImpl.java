package com.edumind.ai.service.chat.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.chat.ChatStreamRegistry;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.exception.BusinessException;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final AiGatewayFacade aiGatewayFacade;
    private final LlmProperties llmProperties;
    private final AiCallLogDao aiCallLogDao;
    private final AiSessionCacheService aiSessionCacheService;
    private final ChatStreamRegistry chatStreamRegistry;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final ApplicationEventPublisher eventPublisher;
    private final IntentDispatchService intentDispatchService;

    @Override
    public SseEmitter streamChat(ChatStreamDTO dto) {
        Long userId = LoginUserResolver.requireUserId();

        ConversationEntity conversation = resolveConversation(dto, userId);
        saveMessage(conversation.getId(), "user", dto.getMessage(), null);
        aiSessionCacheService.trackUserMessage(conversation.getId(), dto.getMessage());

        String streamId = chatStreamRegistry.register();
        SseEmitter emitter = new SseEmitter(llmProperties.getTimeoutMs().longValue());
        sendEvent(emitter, "stream", Map.of("streamId", streamId));
        CompletableFuture.runAsync(() -> doStream(conversation, dto, emitter, streamId));
        return emitter;
    }

    @Override
    public void cancelStream(String streamId) {
        chatStreamRegistry.cancel(streamId);
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

    private void doStream(ConversationEntity conversation, ChatStreamDTO dto, SseEmitter emitter, String streamId) {
        long start = System.currentTimeMillis();
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
            IntentDispatchRequest dispatchRequest = IntentDispatchRequest.builder()
                    .message(dto.getMessage())
                    .courseId(dto.getCourseId())
                    .knowledgeBaseId(useRag ? knowledgeBaseId : null)
                    .documentId(dto.getDocumentId())
                    .intent(intent)
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

            final String promptForLog = userPrompt;
            final List<CitationVO> citationsForSave = citations;
            final boolean ragUsed = useRag && knowledgeBaseId != null;
            aiGatewayFacade.streamChat("CHAT", systemPrompt, userPrompt, new LlmClient.StreamCallback() {
                @Override
                public void onChunk(String content) {
                    if (chatStreamRegistry.isCancelled(streamId)) {
                        return;
                    }
                    assistantContent.append(content);
                    aiSessionCacheService.appendStreamingContent(conversation.getId(), content);
                    sendEvent(emitter, "delta", Map.of("content", content));
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
                            conversation.getId(), "assistant", assistantContent.toString(), citationsJson);
                    updateConversationStats(conversation);
                    aiSessionCacheService.markStreamComplete(conversation.getId(), assistantContent.toString());
                    Map<String, String> done = new HashMap<>();
                    done.put("conversationId", conversation.getId());
                    done.put("messageId", assistantMsg.getId());
                    if (plan.getAgentCode() != null) {
                        done.put("agentCode", plan.getAgentCode());
                    }
                    sendEvent(emitter, "done", done);
                    emitter.complete();
                    logCall(start, ragUsed, knowledgeBaseId, conversation.getCourseId(), promptForLog, assistantContent.toString(), citationsForSave);
                    publishChatActivity(conversation);
                    chatStreamRegistry.remove(streamId);
                }

                @Override
                public void onError(String message) {
                    aiSessionCacheService.deleteSession(conversation.getId());
                    sendEvent(emitter, "error", Map.of("code", "AI_ERROR", "message", message));
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

    private MessageEntity saveMessage(String conversationId, String role, String content, String citationsJson) {
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCitationsJson(citationsJson);
        message.setTokenCount(content != null ? content.length() : 0);
        messageDao.insert(message);
        return message;
    }

    private void updateConversationStats(ConversationEntity conversation) {
        conversation.setMessageCount(
                (conversation.getMessageCount() != null ? conversation.getMessageCount() : 0) + 2);
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
        if ("rag".equalsIgnoreCase(intent.type())) {
            return true;
        }
        return knowledgeBaseId != null && "chat".equalsIgnoreCase(intent.type());
    }

    private void logCall(long start, boolean useRag, Long knowledgeBaseId, Long courseId, String prompt, String completion,
                         List<CitationVO> citations) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setUserId(LoginUserResolver.resolveUserId());
        log.setCourseId(courseId);
        log.setModel(llmProperties.getModel());
        log.setScene(useRag ? "CHAT_RAG" : "chat_stream");
        log.setLatencyMs((int) (System.currentTimeMillis() - start));
        log.setKnowledgeBaseId(knowledgeBaseId);
        log.setPromptTokens(estimateTokens(prompt));
        log.setCompletionTokens(estimateTokens(completion));
        if (citations != null && !citations.isEmpty()) {
            log.setRetrievalHitCount(citations.size());
        }
        aiCallLogDao.insert(log);
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
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
