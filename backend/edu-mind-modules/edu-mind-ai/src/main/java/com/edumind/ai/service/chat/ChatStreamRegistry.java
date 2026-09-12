package com.edumind.ai.service.chat;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatStreamRegistry {

    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    public String register() {
        String streamId = java.util.UUID.randomUUID().toString();
        cancelFlags.put(streamId, new AtomicBoolean(false));
        return streamId;
    }

    public void cancel(String streamId) {
        AtomicBoolean flag = cancelFlags.get(streamId);
        if (flag != null) {
            flag.set(true);
        }
    }

    public boolean isCancelled(String streamId) {
        AtomicBoolean flag = cancelFlags.get(streamId);
        return flag != null && flag.get();
    }

    public void remove(String streamId) {
        cancelFlags.remove(streamId);
    }
}
