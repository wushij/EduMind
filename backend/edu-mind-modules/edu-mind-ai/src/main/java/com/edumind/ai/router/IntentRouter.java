package com.edumind.ai.router;

import java.util.Map;

/**
 * AI 意图识别与能力路由网关 (Chat / RAG / Agent / Tool 等)
 */
public interface IntentRouter {

    String routeIntent(String userMessage);

    IntentResult route(String userMessage, Long courseId);

    record IntentResult(
            String type,            // "chat", "rag", "agent", "tool"
            String targetCode,      // "teaching", "exam", "learning", "paper_compose"
            Double confidence,
            Map<String, Object> slots
    ) {}
}
