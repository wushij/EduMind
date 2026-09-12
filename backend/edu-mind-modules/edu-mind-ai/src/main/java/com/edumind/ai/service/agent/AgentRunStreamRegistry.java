package com.edumind.ai.service.agent;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AgentRunStreamRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    public void register(String runId, SseEmitter emitter) {
        emitters.put(runId, emitter);
        cancelFlags.put(runId, new AtomicBoolean(false));
        emitter.onCompletion(() -> remove(runId));
        emitter.onTimeout(() -> remove(runId));
    }

    public void emit(String runId, String event, Object data) {
        SseEmitter emitter = emitters.get(runId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ex) {
            remove(runId);
        }
    }

    public void complete(String runId) {
        SseEmitter emitter = emitters.remove(runId);
        cancelFlags.remove(runId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public void cancel(String runId) {
        AtomicBoolean flag = cancelFlags.get(runId);
        if (flag != null) {
            flag.set(true);
        }
        complete(runId);
    }

    public boolean isCancelled(String runId) {
        AtomicBoolean flag = cancelFlags.get(runId);
        return flag != null && flag.get();
    }

    public void remove(String runId) {
        emitters.remove(runId);
        cancelFlags.remove(runId);
    }
}
