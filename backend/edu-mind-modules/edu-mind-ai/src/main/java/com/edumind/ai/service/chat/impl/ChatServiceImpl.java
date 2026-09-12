package com.edumind.ai.service.chat.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.chat.ChatStreamRegistry;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
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
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final AiCallLogDao aiCallLogDao;
    private final PromptService promptService;
    private final AiSessionCacheService aiSessionCacheService;
    private final RagPipelineImpl ragPipeline;
    private final ChatStreamRegistry chatStreamRegistry;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final KnowledgeAccessService knowledgeAccessService;

    @Override
    public SseEmitter streamChat(ChatStreamDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }

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
        final RagResult[] ragHolder = new RagResult[1];
        final Long knowledgeBaseId = resolveKnowledgeBaseId(dto);
        final boolean useRag = shouldUseRag(dto, knowledgeBaseId);
        try {
            String systemPrompt = promptService.getSystemPrompt("chat");
            String userPrompt = dto.getMessage();
            List<CitationVO> citations = List.of();
            if (useRag && knowledgeBaseId != null) {
                knowledgeAccessService.assertAccessible(knowledgeBaseId);
                validateKnowledgeBaseCourse(knowledgeBaseId, dto.getCourseId());
                ragHolder[0] = ragPipeline.executeDetailed(
                        dto.getMessage(),
                        knowledgeBaseId,
                        5,
                        0.65,
                        dto.getDocumentId(),
                        false
                );
                userPrompt = ragHolder[0].getPromptPreview();
                citations = toCitations(ragHolder[0]);
                if (!citations.isEmpty()) {
                    sendEvent(emitter, "citation", Map.of("citations", citations));
                }
            }
            final String promptForLog = userPrompt;
            final List<CitationVO> citationsForSave = citations;
            llmClient.streamChat(systemPrompt, userPrompt, new LlmClient.StreamCallback() {
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
                    sendEvent(emitter, "done", done);
                    emitter.complete();
                    logCall(start, useRag, knowledgeBaseId, promptForLog, assistantContent.toString(), ragHolder[0]);
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

    private List<CitationVO> toCitations(RagResult ragResult) {
        if (ragResult == null || ragResult.getRetrievalResults() == null) {
            return List.of();
        }
        return ragResult.getRetrievalResults().stream().map(this::toCitation).collect(Collectors.toList());
    }

    private CitationVO toCitation(RetrievalHit hit) {
        CitationVO citation = new CitationVO();
        citation.setDocumentName(hit.getDocumentName());
        citation.setPageNo(hit.getPageNo());
        citation.setChunkId(hit.getChunkId());
        citation.setScore(hit.getScore());
        citation.setExcerpt(hit.getExcerpt());
        citation.setChunkIndex(hit.getChunkIndex());
        return citation;
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

    private boolean shouldUseRag(ChatStreamDTO dto, Long knowledgeBaseId) {
        if (dto.getUseRag() != null) {
            return dto.getUseRag();
        }
        return knowledgeBaseId != null;
    }

    private void validateKnowledgeBaseCourse(Long knowledgeBaseId, Long courseId) {
        if (courseId == null) {
            return;
        }
        boolean matched = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                .anyMatch(kb -> knowledgeBaseId.equals(kb.getId()));
        if (!matched) {
            throw new BusinessException("知识库与课程不匹配");
        }
    }

    private void logCall(long start, boolean useRag, Long knowledgeBaseId, String prompt, String completion,
                         RagResult ragResult) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setUserId(UserContext.getUserId());
        log.setModel(llmProperties.getModel());
        log.setScene(useRag ? "CHAT_RAG" : "chat_stream");
        log.setLatencyMs((int) (System.currentTimeMillis() - start));
        log.setKnowledgeBaseId(knowledgeBaseId);
        log.setPromptTokens(estimateTokens(prompt));
        log.setCompletionTokens(estimateTokens(completion));
        if (ragResult != null && ragResult.getRetrievalResults() != null) {
            log.setRetrievalHitCount(ragResult.getRetrievalResults().size());
            String docIds = ragResult.getRetrievalResults().stream()
                    .map(hit -> String.valueOf(hit.getDocumentId()))
                    .distinct()
                    .collect(Collectors.joining(","));
            log.setCitationDocIds(docIds);
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
}
