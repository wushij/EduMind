package com.edumind.ai.gateway;

import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
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

    private final LlmClientRegistry llmClientRegistry;
    private final ModelRouterImpl modelRouter;

    @Value("${edumind.ai.gateway.force-fail-model:}")
    private String forceFailModel;

    private final AtomicInteger totalRequests = new AtomicInteger();
    private final AtomicInteger fallbackCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private volatile long totalLatencyMs;

    public String chat(String scene, String systemPrompt, String userPrompt) {
        return chat(scene, null, systemPrompt, userPrompt);
    }

    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        try {
            if (forceFailModel != null && forceFailModel.equals(primary)) {
                throw new IllegalStateException("Mock gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
            totalLatencyMs += System.currentTimeMillis() - start;
            return result;
        } catch (Exception ex) {
            String fallback = modelRouter.resolveFallback(primary);
            if (fallback != null && !fallback.equals(primary)) {
                log.warn("Gateway fallback {} -> {} scene={}", primary, fallback, scene);
                fallbackCount.incrementAndGet();
                try {
                    String result = llmClientRegistry.get(fallback).chat(systemPrompt, userPrompt);
                    totalLatencyMs += System.currentTimeMillis() - start;
                    return result;
                } catch (Exception fallbackEx) {
                    failedCount.incrementAndGet();
                    throw fallbackEx;
                }
            }
            failedCount.incrementAndGet();
            throw ex;
        }
    }

    public String generateQuestions(String scene, String modelKey, String prompt, Map<String, Object> params) {
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        try {
            if (forceFailModel != null && forceFailModel.equals(primary)) {
                throw new IllegalStateException("Mock gateway failure for " + primary);
            }
            return llmClientRegistry.get(primary).generateQuestions(prompt, params);
        } catch (Exception ex) {
            String fallback = modelRouter.resolveFallback(primary);
            fallbackCount.incrementAndGet();
            return llmClientRegistry.get(fallback != null ? fallback : "mock").generateQuestions(prompt, params);
        }
    }

    public void streamChat(String scene, String systemPrompt, String userPrompt, LlmClient.StreamCallback callback) {
        streamChat(scene, null, systemPrompt, userPrompt, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, String userPrompt,
                           LlmClient.StreamCallback callback) {
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        llmClientRegistry.get(primary).streamChat(systemPrompt, userPrompt, callback);
    }

    public GatewayMetrics snapshot() {
        GatewayMetrics m = new GatewayMetrics();
        int total = totalRequests.get();
        m.setTotalRequests(total);
        m.setFallbackCount(fallbackCount.get());
        m.setFailedCount(failedCount.get());
        m.setSuccessRate(total == 0 ? 1.0 : (total - failedCount.get()) * 1.0 / total);
        m.setAvgLatencyMs(total == 0 ? 0 : totalLatencyMs / total);
        return m;
    }

    @lombok.Data
    public static class GatewayMetrics {
        private long totalRequests;
        private long fallbackCount;
        private long failedCount;
        private double successRate;
        private long avgLatencyMs;
    }
}
