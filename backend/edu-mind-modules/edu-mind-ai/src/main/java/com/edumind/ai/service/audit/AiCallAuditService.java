package com.edumind.ai.service.audit;

public interface AiCallAuditService {

    void record(String scene, String modelKey, AiCallAuditContext context, long startMs,
                int promptTokens, int completionTokens);

    void recordEstimated(String scene, String modelKey, AiCallAuditContext context, long startMs,
                         String promptText, String completionText);

    String resolveModelDisplay(String modelKey);
}
