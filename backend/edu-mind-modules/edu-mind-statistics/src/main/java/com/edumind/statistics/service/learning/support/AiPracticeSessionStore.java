package com.edumind.statistics.service.learning.support;

import com.edumind.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiPracticeSessionStore {

    private static final long MAX_AGE_MS = 4 * 60 * 60 * 1000L;

    private final ConcurrentHashMap<String, AiPracticeSessionContext> sessions = new ConcurrentHashMap<>();

    public void save(String sessionId, AiPracticeSessionContext context) {
        sessions.put(sessionId, context);
    }

    public AiPracticeSessionContext require(String sessionId, Long studentId) {
        purgeExpired();
        AiPracticeSessionContext ctx = sessions.get(sessionId);
        if (ctx == null) {
            throw new BusinessException("练习会话已过期，请重新生成练习集");
        }
        if (studentId != null && ctx.getStudentId() != null && !studentId.equals(ctx.getStudentId())) {
            throw new BusinessException("无权访问该练习会话");
        }
        return ctx;
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now - e.getValue().getCreatedAtMs() > MAX_AGE_MS);
    }
}
