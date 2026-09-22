package com.edumind.ai.rag.retrieval;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class VectorRecallLatencyTracker {

    private static final int MAX_HISTORY = 20;
    private final ConcurrentHashMap<Long, ConcurrentLinkedDeque<Long>> kbLatencies = new ConcurrentHashMap<>();

    public void record(Long knowledgeBaseId, long latencyMs) {
        if (knowledgeBaseId == null) {
            return;
        }
        ConcurrentLinkedDeque<Long> queue = kbLatencies.computeIfAbsent(knowledgeBaseId, k -> new ConcurrentLinkedDeque<>());
        queue.addLast(latencyMs);
        while (queue.size() > MAX_HISTORY) {
            queue.pollFirst();
        }
    }

    public long getAverageLatency(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return 0L;
        }
        ConcurrentLinkedDeque<Long> queue = kbLatencies.get(knowledgeBaseId);
        if (queue == null || queue.isEmpty()) {
            return 0L;
        }
        long sum = 0;
        int count = 0;
        for (Long lat : queue) {
            sum += lat;
            count++;
        }
        return count > 0 ? Math.round((double) sum / count) : 0L;
    }
}
