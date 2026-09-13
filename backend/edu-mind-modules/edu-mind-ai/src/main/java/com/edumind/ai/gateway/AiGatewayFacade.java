package com.edumind.ai.gateway;

import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.gateway.GatewayResilienceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGatewayFacade {

    private static final int DEFAULT_RATE_LIMIT = 120;
    private static final String DEFAULT_CIRCUIT_KEY = "default";

    private final LlmClientRegistry llmClientRegistry;
    private final ModelRouterImpl modelRouter;
    private final GatewayResilienceStore resilienceStore;

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
        return chat(scene, null, systemPrompt, userPrompt);
    }

    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt) {
        checkRateLimit(scene != null ? scene : "default", DEFAULT_RATE_LIMIT);
        long start = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            log.warn("Gateway circuit OPEN for {}, fast-falling back", primary);
            return executeFallback(scene, primary, systemPrompt, userPrompt, start);
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Simulated gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
            totalLatencyMs += System.currentTimeMillis() - start;
            resilienceStore.recordSuccess(primary);
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
                return retryResult;
            } catch (Exception retryEx) {
                resilienceStore.recordFailure(primary);
                return executeFallback(scene, primary, systemPrompt, userPrompt, start);
            }
        }
    }

    private String executeFallback(String scene, String primary, String systemPrompt, String userPrompt, long start) {
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            log.warn("Gateway fallback {} -> {} scene={}", primary, fallback, scene);
            resilienceStore.recordFallback();
            try {
                String result = llmClientRegistry.get(fallback).chat(systemPrompt, userPrompt);
                totalLatencyMs += System.currentTimeMillis() - start;
                return result;
            } catch (Exception fallbackEx) {
                resilienceStore.recordFailed();
                throw fallbackEx;
            }
        }
        resilienceStore.recordFailed();
        throw new BusinessException("AI 服务暂不可用，主模型与备用模型均失败");
    }

    public String generateQuestions(String scene, String modelKey, String prompt, Map<String, Object> params) {
        checkRateLimit(scene != null ? scene : "exam", DEFAULT_RATE_LIMIT);
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Mock gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).generateQuestions(prompt, params);
            resilienceStore.recordSuccess(primary);
            return result;
        } catch (Exception ex) {
            resilienceStore.recordFailure(primary);
            String fallback = modelRouter.resolveFallback(primary);
            resilienceStore.recordFallback();
            return llmClientRegistry.get(fallback != null ? fallback : "mock").generateQuestions(prompt, params);
        }
    }

    public void streamChat(String scene, String systemPrompt, String userPrompt, LlmClient.StreamCallback callback) {
        streamChat(scene, null, systemPrompt, userPrompt, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, String userPrompt,
                           LlmClient.StreamCallback callback) {
        checkRateLimit(scene != null ? scene : "stream", DEFAULT_RATE_LIMIT);
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            streamFallback(primary, systemPrompt, userPrompt, callback);
            return;
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Mock gateway stream failure for " + primary);
            }
            llmClientRegistry.get(primary).streamChat(systemPrompt, userPrompt, callback);
            resilienceStore.recordSuccess(primary);
        } catch (Exception ex) {
            log.warn("Gateway stream primary failed for {}: {}", primary, ex.getMessage());
            resilienceStore.recordRetry();
            resilienceStore.recordFailure(primary);
            streamFallback(primary, systemPrompt, userPrompt, callback);
        }
    }

    private void streamFallback(String primary, String systemPrompt, String userPrompt,
                                LlmClient.StreamCallback callback) {
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            resilienceStore.recordFallback();
            llmClientRegistry.get(fallback).streamChat(systemPrompt, userPrompt, callback);
            return;
        }
        resilienceStore.recordFailed();
        throw new BusinessException("AI 流式服务暂不可用");
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
