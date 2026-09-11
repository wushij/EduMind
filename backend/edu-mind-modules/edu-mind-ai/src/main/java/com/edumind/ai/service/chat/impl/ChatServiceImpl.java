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
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public SseEmitter streamChat(ChatStreamDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }

        ConversationEntity conversation = resolveConversation(dto, userId);
        saveMessage(conversation.getId(), "user", dto.getMessage());
        aiSessionCacheService.trackUserMessage(conversation.getId(), dto.getMessage());

        SseEmitter emitter = new SseEmitter(llmProperties.getTimeoutMs().longValue());
        CompletableFuture.runAsync(() -> doStream(conversation, dto.getMessage(), emitter));
        return emitter;
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

    private void doStream(ConversationEntity conversation, String userMessage, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        StringBuilder assistantContent = new StringBuilder();
        try {
            llmClient.streamChat(promptService.getSystemPrompt("chat"), userMessage, new LlmClient.StreamCallback() {
                @Override
                public void onChunk(String content) {
                    assistantContent.append(content);
                    aiSessionCacheService.appendStreamingContent(conversation.getId(), content);
                    sendEvent(emitter, "message", Map.of("content", content));
                }

                @Override
                public void onComplete() {
                    MessageEntity assistantMsg = saveMessage(conversation.getId(), "assistant", assistantContent.toString());
                    updateConversationStats(conversation);
                    aiSessionCacheService.markStreamComplete(conversation.getId(), assistantContent.toString());
                    Map<String, String> done = new HashMap<>();
                    done.put("conversationId", conversation.getId());
                    done.put("messageId", assistantMsg.getId());
                    sendEvent(emitter, "done", done);
                    emitter.complete();
                    logCall(start);
                }

                @Override
                public void onError(String message) {
                    aiSessionCacheService.deleteSession(conversation.getId());
                    sendEvent(emitter, "error", Map.of("code", "AI_ERROR", "message", message));
                    emitter.completeWithError(new BusinessException(message));
                }
            });
        } catch (Exception ex) {
            aiSessionCacheService.deleteSession(conversation.getId());
            sendEvent(emitter, "error", Map.of("code", "AI_TIMEOUT", "message", ex.getMessage()));
            emitter.completeWithError(ex);
        }
    }

    private MessageEntity saveMessage(String conversationId, String role, String content) {
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
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

    private void logCall(long start) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setUserId(UserContext.getUserId());
        log.setModel(llmProperties.getModel());
        log.setScene("chat_stream");
        log.setLatencyMs((int) (System.currentTimeMillis() - start));
        aiCallLogDao.insert(log);
    }

    private String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "新会话";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
