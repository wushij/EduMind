package com.edumind.ai.gateway;

import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmChatOptions;
import com.edumind.ai.integration.llm.LlmCallCancelledException;
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
import java.util.function.BooleanSupplier;
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
        return chatWithMeta(scene, modelKey, systemPrompt, userPrompt, null, auditContext).content();
    }

    /**
     * 可取消的对话调用。
     *
     * <p>{@code cancelled} 会被 LLM 客户端在流式读取过程中轮询，置位后立即关闭上游响应流，
     * 用于「用户点击中止后马上停止后台调用与计费」；中止不会触发重试或 fallback 模型。</p>
     */
    public String chat(String scene, String modelKey, String systemPrompt, String userPrompt,
                       AiCallAuditContext auditContext, BooleanSupplier cancelled) {
        return chatWithMeta(scene, modelKey, systemPrompt, userPrompt, null, auditContext, cancelled).content();
    }

    /**
     * 带调用元信息的对话调用（诊断工作台需要知道「实际命中的模型」与覆盖温度）。
     *
     * <p>除返回值外，行为与 {@link #chat(String, String, String, String, AiCallAuditContext)} 完全一致：
     * 熔断快速回落、失败重试一次、再失败走 fallback 链路，审计与配额口径不变。</p>
     *
     * @param options 生成参数覆盖（温度等）；传 null 时调用 SDK 的默认重载，保持与既有链路一致
     * @return 实际命中的模型键 + 生成结果
     */
    public ChatOutcome chatWithMeta(String scene, String modelKey, String systemPrompt, String userPrompt,
                                    LlmChatOptions options, AiCallAuditContext auditContext) {
        return chatWithMeta(scene, modelKey, systemPrompt, userPrompt, options, auditContext, null);
    }

    public ChatOutcome chatWithMeta(String scene, String modelKey, String systemPrompt, String userPrompt,
                                    LlmChatOptions options, AiCallAuditContext auditContext,
                                    BooleanSupplier cancelled) {
        checkRateLimit(scene != null ? scene : "default", DEFAULT_RATE_LIMIT);
        throwIfCancelled(cancelled);
        long start = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            log.warn("Gateway circuit OPEN for {}, fast-falling back", primary);
            return executeFallback(scene, primary, systemPrompt, userPrompt, options, start, auditContext,
                    cancelled, new IllegalStateException("模型熔断保护中，请稍后重试"));
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Simulated gateway failure for " + primary);
            }
            String result = invoke(primary, systemPrompt, userPrompt, options, cancelled);
            totalLatencyMs += System.currentTimeMillis() - start;
            resilienceStore.recordSuccess(primary);
            auditChat(scene, primary, auditContext, start, systemPrompt, userPrompt, result);
            return new ChatOutcome(primary, result);
        } catch (LlmCallCancelledException cancelledEx) {
            // 用户主动中止：不算模型故障，不重试也不换模型，直接向上抛出
            throw cancelledEx;
        } catch (Exception ex) {
            log.warn("Gateway primary failed for {}: {}", primary, ex.getMessage());
            resilienceStore.recordRetry();
            try {
                if (shouldForceFail(primary)) {
                    throw new IllegalStateException("Simulated gateway failure on retry for " + primary);
                }
                String retryResult = invoke(primary, systemPrompt, userPrompt, options, cancelled);
                totalLatencyMs += System.currentTimeMillis() - start;
                resilienceStore.recordSuccess(primary);
                auditChat(scene, primary, auditContext, start, systemPrompt, userPrompt, retryResult);
                return new ChatOutcome(primary, retryResult);
            } catch (LlmCallCancelledException cancelledEx) {
                throw cancelledEx;
            } catch (Exception retryEx) {
                resilienceStore.recordFailure(primary);
                return executeFallback(scene, primary, systemPrompt, userPrompt, options, start, auditContext,
                        cancelled, retryEx);
            }
        }
    }

    /**
     * 未指定覆盖参数时走 SDK 默认重载，避免空 options 改变既有模型参数行为；
     * 仅在真正需要取消的业务上才切到带 cancelled 的新重载。
     */
    private String invoke(String modelKey, String systemPrompt, String userPrompt, LlmChatOptions options,
                          BooleanSupplier cancelled) {
        throwIfCancelled(cancelled);
        LlmClient client = llmClientRegistry.get(modelKey);
        if (cancelled == null) {
            return options == null
                    ? client.chat(systemPrompt, userPrompt)
                    : client.chat(systemPrompt, userPrompt, options);
        }
        return client.chat(systemPrompt, userPrompt,
                options != null ? options : LlmChatOptions.empty(), cancelled);
    }

    /** 中止是用户的明确指令，不是模型故障，因此不参与重试与熔断统计 */
    private void throwIfCancelled(BooleanSupplier cancelled) {
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new LlmCallCancelledException("大模型调用已被用户中止");
        }
    }

    private ChatOutcome executeFallback(String scene, String primary, String systemPrompt, String userPrompt,
                                        LlmChatOptions options, long start, AiCallAuditContext auditContext,
                                        BooleanSupplier cancelled, Exception cause) {
        throwIfCancelled(cancelled);
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            log.warn("Gateway fallback {} -> {} scene={}", primary, fallback, scene);
            resilienceStore.recordFallback();
            try {
                String result = invoke(fallback, systemPrompt, userPrompt, options, cancelled);
                totalLatencyMs += System.currentTimeMillis() - start;
                auditChat(scene, fallback, auditContext, start, systemPrompt, userPrompt, result);
                return new ChatOutcome(fallback, result);
            } catch (LlmCallCancelledException cancelledEx) {
                throw cancelledEx;
            } catch (Exception fallbackEx) {
                resilienceStore.recordFailed();
                throw fallbackEx;
            }
        }
        resilienceStore.recordFailed();
        throw new BusinessException(buildUnavailableMessage(cause));
    }

    /** 对话调用结果：content 为生成内容，modelKey 为实际命中的模型配置键（可能经回落切换） */
    public record ChatOutcome(String modelKey, String content) {
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
        streamChat(scene, modelKey, systemPrompt, messages, auditContext, null, callback);
    }

    public void streamChat(String scene, String modelKey, String systemPrompt, List<LlmChatMessage> messages,
                           AiCallAuditContext auditContext, BooleanSupplier cancelled,
                           LlmClient.StreamCallback callback) {
        checkRateLimit(scene != null ? scene : "stream", DEFAULT_RATE_LIMIT);
        totalRequests.incrementAndGet();
        long start = System.currentTimeMillis();
        String primary = modelRouter.resolveModelKey(scene, modelKey);

        if (resilienceStore.isCircuitOpen(primary)) {
            streamFallback(scene, primary, systemPrompt, messages, auditContext, start, cancelled, callback,
                    new IllegalStateException("模型熔断保护中，请稍后重试"));
            return;
        }

        try {
            if (shouldForceFail(primary)) {
                throw new IllegalStateException("Mock gateway stream failure for " + primary);
            }
            llmClientRegistry.get(primary).streamChatWithHistory(
                    systemPrompt, messages, cancelled,
                    wrapStreamAudit(scene, primary, auditContext, start, systemPrompt, messages, callback));
            resilienceStore.recordSuccess(primary);
        } catch (Exception ex) {
            log.warn("Gateway stream primary failed for {}: {}", primary, ex.getMessage());
            resilienceStore.recordRetry();
            resilienceStore.recordFailure(primary);
            streamFallback(scene, primary, systemPrompt, messages, auditContext, start, cancelled, callback, ex);
        }
    }

    private void streamFallback(String scene, String primary, String systemPrompt, List<LlmChatMessage> messages,
                                AiCallAuditContext auditContext, long start, BooleanSupplier cancelled,
                                LlmClient.StreamCallback callback, Exception cause) {
        String fallback = modelRouter.resolveFallback(primary);
        if (fallback != null && !fallback.equals(primary)) {
            resilienceStore.recordFallback();
            llmClientRegistry.get(fallback).streamChatWithHistory(
                    systemPrompt, messages, cancelled,
                    wrapStreamAudit(scene, fallback, auditContext, start, systemPrompt, messages, callback));
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
