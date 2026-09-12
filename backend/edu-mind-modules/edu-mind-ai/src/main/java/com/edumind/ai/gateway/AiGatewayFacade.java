package com.edumind.ai.gateway;

import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final AtomicInteger circuitOpenCount = new AtomicInteger();
    private final AtomicInteger rateLimitedCount = new AtomicInteger();
    private final AtomicInteger retryCount = new AtomicInteger();
    private volatile long totalLatencyMs;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounters = new ConcurrentHashMap<>();
    private volatile long currentMinute = System.currentTimeMillis() / 60000;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long circuitOpenUntilMs = 0;

    /**
     * 动态设置强制失败模型（用于单元测试与故障注入演练）
     */
    public void setForceFailModel(String model) {
        this.forceFailModel = model;
    }

    public boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntilMs;
    }

    public void forceOpenCircuit() {
        circuitOpenUntilMs = System.currentTimeMillis() + 60000;
        circuitOpenCount.incrementAndGet();
    }

    public void resetCircuit() {
        circuitOpenUntilMs = 0;
        consecutiveFailures.set(0);
    }

    public void checkRateLimit(String key, int maxRequestsPerMinute) {
        long minute = System.currentTimeMillis() / 60000;
        if (minute != currentMinute) {
            synchronized (this) {
                if (minute != currentMinute) {
                    requestCounters.clear();
                    currentMinute = minute;
                }
            }
        }
        AtomicInteger counter = requestCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > maxRequestsPerMinute) {
            rateLimitedCount.incrementAndGet();
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

    public void recordCircuitOpen() {
        circuitOpenCount.incrementAndGet();
    }

    public void recordRateLimited() {
        rateLimitedCount.incrementAndGet();
    }

    public void recordRetry() {
        retryCount.incrementAndGet();
    }

    public String chat(String scene, String systemPrompt, String userPrompt) {
        return chat(scene, null, systemPrompt, userPrompt);
    }

    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt) {
        // 1. 真实限流拦截检查 (每分钟上限 120)
        checkRateLimit(scene != null ? scene : "default", 120);

        long start = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        // 2. 熔断判定：若当前熔断器已开启，直接跳过主模型进入 Fallback
        if (isCircuitOpen()) {
            log.warn("Gateway circuit is OPEN until {}, fast-falling back from {}", circuitOpenUntilMs, primary);
            return executeFallback(scene, primary, systemPrompt, userPrompt, start);
        }

        // 3. 主模型调用 + 故障重试机制
        try {
            if (forceFailModel != null && forceFailModel.equalsIgnoreCase(primary)) {
                throw new IllegalStateException("Simulated gateway failure for " + primary);
            }
            String result = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
            totalLatencyMs += System.currentTimeMillis() - start;
            consecutiveFailures.set(0);
            return result;
        } catch (Exception ex) {
            log.warn("Gateway primary call failed for {}: {}. Attempting retry...", primary, ex.getMessage());
            // 尝试 1 次快速重试
            retryCount.incrementAndGet();
            try {
                if (forceFailModel != null && forceFailModel.equalsIgnoreCase(primary)) {
                    throw new IllegalStateException("Simulated gateway failure on retry for " + primary);
                }
                String retryResult = llmClientRegistry.get(primary).chat(systemPrompt, userPrompt);
                totalLatencyMs += System.currentTimeMillis() - start;
                consecutiveFailures.set(0);
                return retryResult;
            } catch (Exception retryEx) {
                // 重试依然失败，触发连续失败计数并尝试触发熔断
                if (consecutiveFailures.incrementAndGet() >= 3) {
                    forceOpenCircuit();
                    log.error("Consecutive failures >= 3, opened circuit breaker for 60s");
                }
                // 执行 Fallback
                return executeFallback(scene, primary, systemPrompt, userPrompt, start);
            }
        }
    }

    private String executeFallback(String scene, String primary, String systemPrompt, String userPrompt, long start) {
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
        throw new BusinessException("AI 服务暂不可用，主模型与备用模型均失败");
    }

    public String generateQuestions(String scene, String modelKey, String prompt, Map<String, Object> params) {
        checkRateLimit(scene != null ? scene : "exam", 120);
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);
        try {
            if (forceFailModel != null && forceFailModel.equalsIgnoreCase(primary)) {
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
        // 1. 真实限流检查
        checkRateLimit(scene != null ? scene : "stream", 120);
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        // 2. 熔断状态直接降级
        if (isCircuitOpen()) {
            String fallback = modelRouter.resolveFallback(primary);
            log.warn("Gateway circuit is OPEN, stream chat falling back {} -> {}", primary, fallback);
            fallbackCount.incrementAndGet();
            llmClientRegistry.get(fallback != null ? fallback : "mock").streamChat(systemPrompt, userPrompt, callback);
            return;
        }

        // 3. 主模型流式生成
        try {
            if (forceFailModel != null && forceFailModel.equalsIgnoreCase(primary)) {
                throw new IllegalStateException("Mock gateway stream failure for " + primary);
            }
            llmClientRegistry.get(primary).streamChat(systemPrompt, userPrompt, callback);
        } catch (Exception ex) {
            log.warn("Gateway stream primary failed for {}: {}. Attempting fallback...", primary, ex.getMessage());
            retryCount.incrementAndGet();
            String fallback = modelRouter.resolveFallback(primary);
            if (fallback != null && !fallback.equals(primary)) {
                fallbackCount.incrementAndGet();
                try {
                    llmClientRegistry.get(fallback).streamChat(systemPrompt, userPrompt, callback);
                    return;
                } catch (Exception fbEx) {
                    failedCount.incrementAndGet();
                    throw fbEx;
                }
            }
            failedCount.incrementAndGet();
            throw ex;
        }
    }

    public GatewayMetrics snapshot() {
        GatewayMetrics m = new GatewayMetrics();
        int total = totalRequests.get();
        m.setTotalRequests(total);
        m.setFallbackCount(fallbackCount.get());
        m.setFailedCount(failedCount.get());
        m.setSuccessRate(total == 0 ? 1.0 : (total - failedCount.get()) * 1.0 / total);
        m.setAvgLatencyMs(total == 0 ? 0 : totalLatencyMs / Math.max(1, total));
        m.setCircuitOpenCount(circuitOpenCount.get());
        m.setRateLimitedCount(rateLimitedCount.get());
        m.setRetryCount(retryCount.get());
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
