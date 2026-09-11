package com.edumind.ai.router;

/**
 * AI 意图识别与能力路由网关 (Chat / RAG / Agent / 出题批改等)
 */
public interface IntentRouter {
    String routeIntent(String userMessage);
}
