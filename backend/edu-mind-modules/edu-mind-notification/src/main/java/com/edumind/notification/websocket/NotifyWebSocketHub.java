package com.edumind.notification.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 按用户 ID 索引的 WebSocket 会话注册中心，支持多标签页同时在线。
 */
@Slf4j
@Component
public class NotifyWebSocketHub {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("Notification WS connected: userId={}, sessionId={}", userId, session.getId());
    }

    public void unregister(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
        log.debug("Notification WS disconnected: userId={}, sessionId={}", userId, session.getId());
    }

    public void pushToUser(Long userId, String payload) {
        if (userId == null || payload == null || payload.isBlank()) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                userSessions.remove(session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException ex) {
                log.warn("Failed to push notification WS message: userId={}, sessionId={}",
                        userId, session.getId(), ex);
                closeQuietly(session);
                userSessions.remove(session);
            }
        }
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
            // ignore
        }
    }
}
