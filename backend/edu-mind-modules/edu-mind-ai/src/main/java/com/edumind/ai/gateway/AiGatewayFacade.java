package com.edumind.ai.gateway;

import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.audit.AiCallAuditService;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.gateway.GatewayResilienceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGatewayFacade {

    private static final int DEFAULT_RATE_LIMIT = 120;
    private static final String DEFAULT_CIRCUIT_KEY = "default";

    private final LlmClientRegistry llmClientRegistry;
    private final ModelRouter modelRouter;
    private final GatewayResilienceStore resilienceStore;
    private final AiCallAuditService aiCallAuditService;

    @Value("${edumind.ai.gateway.force-fail-model:}")
    private String forceFailModel;

    private final AtomicInteger totalRequests = new AtomicInteger();
    private volatile long totalLatencyMs;

    public void setForceFailModel(String model) {
        this.forceFailModel = model;
    }

    public boolean isCircuitOpen() {
        return resilienceStore.isCircuitOpen(DEFAULT_CIRCUIT_KEY);
    }

    public void forceOpenCircuit() {
        resilienceStore.forceOpenCircuit(DEFAULT_CIRCUIT_KEY);
    }

    public void resetCircuit() {
        resilienceStore.resetCircuit(DEFAULT_CIRCUIT_KEY);
    }

    public void checkRateLimit(String key, int maxRequestsPerMinute) {
        resilienceStore.checkRateLimit(key != null ? key : "default", maxRequestsPerMinute);
    }

    public String chat(String scene, String systemPrompt, String userPrompt) {
        return chat(scene, null, systemPrompt, userPrompt, null);
    }

    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt) {
        return chat(scene, modelKey, systemPrompt, userPrompt, null);
    }

    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt,
                       AiCallAuditContext auditContext) {
        checkRateLimit(scene != null ? scene : "default", DEFAULT_RATE_LIMIT);
        long start = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            log.warn("Gateway circuit OPEN for {}, fast-falling back", primary);
            return executeFallback(scene, primary, systemPrompt, userPrompt, start, auditContext,
                    new IllegalStateException("模型熔断保护中，请稍后重试"));
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Simulated gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
            totalLatencyMs += System.currentTimeMillis() - start;
            resilienceStore.recordSuccess(primary);
            auditChat(scene, primary, auditContext, start, systemPrompt, userPrompt, result);
            return result;
        } catch (Exception ex) {
            log.warn("Gateway primary failed for {}: {}", primary, ex.getMessage());
            resilienceStore.recordRetry();
            try {
                if (shouldForceFail(primary)) {
                    throw new IllegalStateException("Simulated gateway failure on retry for " + primary);
                }
                String retryResult = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
                totalLatencyMs += System.currentTimeMillis() - start;
                resilienceStore.recordSuccess(primary);
                auditChat(scene, primary, auditContext, start, systemPrompt, userPrompt, retryResult);
                return retryResult;
            } catch (Exception retryEx) {
                resilienceStore.recordFailure(primary);
                return executeFallback(scene, primary, systemPrompt, userPrompt, start, auditContext, retryEx);
            }
        }
    }

    private String executeFallback(String scene, String primary, String systemPrompt, String userPrompt,
                                   long start, AiCallAuditContext auditContext, Exception cause) {
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            log.warn("Gateway fallback {} -> {} scene={}", primary, fallback, scene);
            resilienceStore.recordFallback();
            try {
                String result = llmClientRegistry.get(fallback).chat(systemPrompt, userPrompt);
                totalLatencyMs += System.currentTimeMillis() - start;
                auditChat(scene, fallback, auditContext, start, systemPrompt, userPrompt, result);
                return result;
            } catch (Exception fallbackEx) {
                resilienceStore.recordFailed();
                throw fallbackEx;
            }
        }
        resilienceStore.recordFailed();
        throw new BusinessException(buildUnavailableMessage(cause));
    }

    private void auditChat(String scene, String modelKey, AiCallAuditContext auditContext, long start,
                           String systemPrompt, String userPrompt, String result) {
        aiCallAuditService.recordEstimated(scene, modelKey, auditContext, start,
                joinPrompt(systemPrompt, userPrompt), result);
    }

    private String buildUnavailableMessage(Exception cause) {
        if (cause != null && StringUtils.hasText(cause.getMessage())) {
            return "AI 服务暂不可用：" + cause.getMessage();
        }
        return "AI 服务暂不可用，请稍后重试";
    }

    public String generateQuestions(String scene, String modelKey, String prompt, Map<String, Object> params) {
        return generateQuestions(scene, modelKey, prompt, params, null);
    }

    public String generateQuestions(String scene, String modelKey, String prompt, Map<String, Object> params,
                                    AiCallAuditContext auditContext) {
        checkRateLimit(scene != null ? scene : "exam", DEFAULT_RATE_LIMIT);
        totalRequests.incrementAndGet();
        long start = System.currentTimeMillis();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Mock gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).generateQuestions(prompt, params);
            resilienceStore.recordSuccess(primary);
            aiCallAuditService.recordEstimated("question_generate", primary, auditContext, start, prompt, result);
            return result;
        } catch (Exception ex) {
            resilienceStore.recordFailure(primary);
            String fallback = modelRouter.resolveFallback(primary);
            if (StringUtils.hasText(fallback) && !fallback.equals(primary)) {
                resilienceStore.recordFallback();
                String result = llmClientRegistry.get(fallback).generateQuestions(prompt, params);
                aiCallAuditService.recordEstimated("question_generate", fallback, auditContext, start, prompt, result);
                return result;
            }
            throw new BusinessException(buildUnavailableMessage(ex));
        }
    }

    public void streamChat(String scene, String systemPrompt, String userPrompt, LlmClient.StreamCallback callback) {
        streamChat(scene, null, systemPrompt, userPrompt, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, String userPrompt,
                           LlmClient.StreamCallback callback) {
        streamChat(scene, modelKey, systemPrompt, userPrompt, null, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, String userPrompt,
                           AiCallAuditContext auditContext, LlmClient.StreamCallback callback) {
        streamChat(scene, modelKey, systemPrompt, List.of(LlmChatMessage.user(userPrompt)), auditContext, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, List<LlmChatMessage> messages,
                           LlmClient.StreamCallback callback) {
        streamChat(scene, modelKey, systemPrompt, messages, null, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, List<LlmChatMessage> messages,
                           AiCallAuditContext auditContext, LlmClient.StreamCallback callback) {
        checkRateLimit(scene != null ? scene : "stream", DEFAULT_RATE_LIMIT);
        totalRequests.incrementAndGet();
        long start = System.currentTimeMillis();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            streamFallback(scene, primary, systemPrompt, messages, auditContext, start, callback,
                    new IllegalStateException("模型熔断保护中，请稍后重试"));
            return;
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Mock gateway stream failure for " + primary);
            }
            llmClientRegistry.get(primary).streamChatWithHistory(
                    systemPrompt, messages, wrapStreamAudit(scene, primary, auditContext, start, systemPrompt, messages, callback));
            resilienceStore.recordSuccess(primary);
        } catch (Exception ex) {
            log.warn("Gateway stream primary failed for {}: {}", primary, ex.getMessage());
            resilienceStore.recordRetry();
            resilienceStore.recordFailure(primary);
            streamFallback(scene, primary, systemPrompt, messages, auditContext, start, callback, ex);
        }
    }

    private void streamFallback(String scene, String primary, String systemPrompt, List<LlmChatMessage> messages,
                                AiCallAuditContext auditContext, long start, LlmClient.StreamCallback callback,
                                Exception cause) {
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            resilienceStore.recordFallback();
            llmClientRegistry.get(fallback).streamChatWithHistory(
                    systemPrompt, messages, wrapStreamAudit(scene, fallback, auditContext, start, systemPrompt, messages, callback));
            return;
        }
        resilienceStore.recordFailed();
        callback.onError(buildUnavailableMessage(cause));
    }

    private LlmClient.StreamCallback wrapStreamAudit(String scene, String modelKey, AiCallAuditContext auditContext,
                                                     long start, String systemPrompt, List<LlmChatMessage> messages,
                                                     LlmClient.StreamCallback callback) {
        StringBuilder completion = new StringBuilder();
        AtomicReference<String> usedModelKey = new AtomicReference<>(modelKey);
        return new LlmClient.StreamCallback() {
            @Override
            public void onReasoning(String content) {
                callback.onReasoning(content);
            }

            @Override
            public void onChunk(String content) {
                completion.append(content);
                callback.onChunk(content);
            }

            @Override
            public void onStatus(String phase, String message) {
                callback.onStatus(phase, message);
            }

            @Override
            public void onComplete() {
                aiCallAuditService.recordEstimated(scene, usedModelKey.get(), auditContext, start,
                        joinHistoryPrompt(systemPrompt, messages), completion.toString());
                callback.onComplete();
            }

            @Override
            public void onError(String message) {
                aiCallAuditService.recordEstimated(scene, usedModelKey.get(), auditContext, start,
                        joinHistoryPrompt(systemPrompt, messages), completion.toString());
                callback.onError(message);
            }
        };
    }

    private String joinPrompt(String systemPrompt, String userPrompt) {
        return (systemPrompt != null ? systemPrompt : "") + "\n" + (userPrompt != null ? userPrompt : "");
    }

    private String joinHistoryPrompt(String systemPrompt, List<LlmChatMessage> messages) {
        StringBuilder builder = new StringBuilder(systemPrompt != null ? systemPrompt : "");
        if (messages != null) {
            for (LlmChatMessage message : messages) {
                if (message != null && StringUtils.hasText(message.getContent())) {
                    builder.append('\n').append(message.getContent());
                }
            }
        }
        return builder.toString();
    }

    private boolean shouldForceFail(String modelKey) {
        return forceFailModel != null && forceFailModel.equalsIgnoreCase(modelKey);
    }

    public GatewayMetrics snapshot() {
        GatewayMetrics m = new GatewayMetrics();
        int total = totalRequests.get();
        long failed = resilienceStore.getMetric("failedCount");
        m.setTotalRequests(total);
        m.setFallbackCount(resilienceStore.getMetric("fallbackCount"));
        m.setFailedCount(failed);
        m.setSuccessRate(total == 0 ? 1.0 : (total - failed) * 1.0 / total);
        m.setAvgLatencyMs(total == 0 ? 0 : totalLatencyMs / Math.max(1, total));
        m.setCircuitOpenCount(resilienceStore.getMetric("circuitOpenCount"));
        m.setRateLimitedCount(resilienceStore.getMetric("rateLimitedCount"));
        m.setRetryCount(resilienceStore.getMetric("retryCount"));
        return m;
    }

    @lombok.Data
    public static class GatewayMetrics {
        private long totalRequests;
        private long fallbackCount;
        private long failedCount;
        private double successRate;
        private long avgLatencyMs;
        private long circuitOpenCount;
        private long rateLimitedCount;
        private long retryCount;
    }
}
