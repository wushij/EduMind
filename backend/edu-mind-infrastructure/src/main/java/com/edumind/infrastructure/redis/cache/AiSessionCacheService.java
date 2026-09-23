package com.edumind.infrastructure.redis.cache;

import com.edumind.common.constant.RedisConstant;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiSessionCacheService {

    private final RedisService redisService;

    public AiSessionState getSession(String conversationId) {
        return redisService.getObject(RedisKeyBuilder.aiSession(conversationId), AiSessionState.class);
    }

    public void saveSession(AiSessionState state) {
        if (state == null || state.getConversationId() == null) {
            return;
        }
        redisService.setObject(
                RedisKeyBuilder.aiSession(state.getConversationId()),
                state,
                RedisConstant.AI_SESSION_TTL_SECONDS);
    }

    public void deleteSession(String conversationId) {
        if (conversationId == null) {
            return;
        }
        redisService.delete(RedisKeyBuilder.aiSession(conversationId));
    }

    public boolean tryStartGenerating(String biz, Long userId, long ttlSeconds) {
        return redisService.tryAcquire(RedisKeyBuilder.aiGenerating(biz, userId), ttlSeconds);
    }

    public void finishGenerating(String biz, Long userId) {
        redisService.release(RedisKeyBuilder.aiGenerating(biz, userId));
    }

    /**
     * 标记该业务的本次生成已被用户主动中止。
     *
     * <p>互斥锁只负责「不允许重复发起」，无法让已经在跑的调用停下来；
     * LLM 调用链路需要轮询本标记，才能在读到置位后立即断开与上游的连接、停止计费。</p>
     */
    public void markCancelled(String biz, Long userId, long ttlSeconds) {
        redisService.set(RedisKeyBuilder.aiCancelled(biz, userId), "1", ttlSeconds);
    }

    public boolean isCancelled(String biz, Long userId) {
        return redisService.get(RedisKeyBuilder.aiCancelled(biz, userId)) != null;
    }

    public void clearCancelled(String biz, Long userId) {
        redisService.delete(RedisKeyBuilder.aiCancelled(biz, userId));
    }

    public void appendStreamingContent(String conversationId, String chunk) {
        AiSessionState state = getSession(conversationId);
        if (state == null) {
            state = new AiSessionState();
            state.setConversationId(conversationId);
        }
        state.setStreaming(true);
        state.setStreamingContent(
                (state.getStreamingContent() == null ? "" : state.getStreamingContent()) + chunk);
        saveSession(state);
    }

    public void markStreamComplete(String conversationId, String assistantContent) {
        AiSessionState state = getSession(conversationId);
        if (state == null) {
            state = new AiSessionState();
            state.setConversationId(conversationId);
        }
        state.setStreaming(false);
        state.setStreamingContent(assistantContent);
        if (state.getRecentMessages() == null) {
            state.setRecentMessages(new ArrayList<>());
        }
        state.getRecentMessages().add(new MessageSnapshot("assistant", assistantContent));
        while (state.getRecentMessages().size() > 10) {
            state.getRecentMessages().remove(0);
        }
        saveSession(state);
    }

    public void trackUserMessage(String conversationId, String content) {
        AiSessionState state = getSession(conversationId);
        if (state == null) {
            state = new AiSessionState();
            state.setConversationId(conversationId);
        }
        if (state.getRecentMessages() == null) {
            state.setRecentMessages(new ArrayList<>());
        }
        state.getRecentMessages().add(new MessageSnapshot("user", content));
        while (state.getRecentMessages().size() > 10) {
            state.getRecentMessages().remove(0);
        }
        saveSession(state);
    }

    @Data
    public static class AiSessionState {
        private String conversationId;
        private Long userId;
        private Long courseId;
        private boolean streaming;
        private String streamingContent;
        private List<MessageSnapshot> recentMessages = new ArrayList<>();
    }

    @Data
    public static class MessageSnapshot {
        private String role;
        private String content;

        public MessageSnapshot() {
        }

        public MessageSnapshot(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
