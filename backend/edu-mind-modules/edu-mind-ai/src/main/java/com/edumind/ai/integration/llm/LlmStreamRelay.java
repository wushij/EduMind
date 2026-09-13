package com.edumind.ai.integration.llm;

import com.edumind.ai.util.ReasoningStreamLimiter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 将 LlmClient 流式回调转为 SSE 事件（reasoning / status / message|delta），对齐 goblog copilot。
 */
public final class LlmStreamRelay {

    private LlmStreamRelay() {
    }

    public static LlmClient.StreamCallback create(
            BiConsumer<String, Map<String, Object>> sendEvent,
            StringBuilder contentBuffer,
            StringBuilder reasoningBuffer) {
        AtomicBoolean contentStarted = new AtomicBoolean(false);
        AtomicBoolean reasoningCapped = new AtomicBoolean(false);
        int[] reasoningSent = {0};

        return new LlmClient.StreamCallback() {
            @Override
            public void onReasoning(String chunk) {
                if (chunk == null || chunk.isEmpty()) {
                    return;
                }
                reasoningBuffer.append(chunk);
                int prev = reasoningSent[0];
                ReasoningStreamLimiter.DeltaResult delta = ReasoningStreamLimiter.takeDelta(prev, chunk);
                reasoningSent[0] = delta.newTotal();
                if (!delta.delta().isEmpty()) {
                    sendEvent.accept("reasoning", Map.of("content", delta.delta()));
                }
                if (!reasoningCapped.get()
                        && prev < ReasoningStreamLimiter.MAX_REASONING_STREAM_RUNES
                        && reasoningSent[0] >= ReasoningStreamLimiter.MAX_REASONING_STREAM_RUNES
                        && !contentStarted.get()) {
                    reasoningCapped.set(true);
                    sendEvent.accept("status", Map.of(
                            "phase", "composing",
                            "message", "思考要点已展示，正在继续推演并撰写回答正文…"
                    ));
                }
            }

            @Override
            public void onChunk(String chunk) {
                if (chunk == null || chunk.isEmpty()) {
                    return;
                }
                if (!contentStarted.get() && reasoningBuffer.length() > 0) {
                    contentStarted.set(true);
                    sendEvent.accept("status", Map.of(
                            "phase", "composing",
                            "message", "思考已完成，正在撰写回答正文…"
                    ));
                }
                contentBuffer.append(chunk);
                Map<String, Object> payload = new HashMap<>();
                payload.put("content", chunk);
                sendEvent.accept("delta", payload);
            }

            @Override
            public void onStatus(String phase, String message) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("phase", phase);
                if (message != null) {
                    payload.put("message", message);
                }
                sendEvent.accept("status", payload);
            }

            @Override
            public void onError(String message) {
                sendEvent.accept("error", Map.of("message", message != null ? message : "AI 服务异常"));
            }
        };
    }
}
