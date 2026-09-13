package com.edumind.ai.util;

/**
 * 流式推送给前端的思考展示上限（完整思考仍入库，仅 UI 截断防刷屏）。
 * 流式思考展示上限（完整思考仍入库）。
 */
public final class ReasoningStreamLimiter {

    public static final int MAX_REASONING_STREAM_RUNES = 16000;

    private ReasoningStreamLimiter() {
    }

    public record DeltaResult(String delta, int newTotal) {
    }

    public static DeltaResult takeDelta(int alreadySent, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return new DeltaResult("", alreadySent);
        }
        if (alreadySent >= MAX_REASONING_STREAM_RUNES) {
            return new DeltaResult("", alreadySent);
        }
        int[] codePoints = chunk.codePoints().toArray();
        int remain = MAX_REASONING_STREAM_RUNES - alreadySent;
        if (codePoints.length <= remain) {
            return new DeltaResult(chunk, alreadySent + codePoints.length);
        }
        String truncated = new String(codePoints, 0, remain) + "…";
        return new DeltaResult(truncated, MAX_REASONING_STREAM_RUNES);
    }
}
