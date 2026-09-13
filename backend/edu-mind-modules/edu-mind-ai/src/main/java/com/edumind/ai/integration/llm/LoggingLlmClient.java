package com.edumind.ai.integration.llm;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.cache.AiQuotaService;
import com.edumind.system.api.TenantQuotaApi;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class LoggingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final AiCallLogDao aiCallLogDao;
    private final LlmProperties properties;
    private final AiQuotaService aiQuotaService;
    private final long dailyQuota;
    private final TenantQuotaApi tenantQuotaApi;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.chat(systemPrompt, userPrompt);
            logCall("chat", start, estimateTokens(systemPrompt, userPrompt), estimateTokens(result));
            return result;
        } catch (RuntimeException ex) {
            logCall("chat", start, estimateTokens(systemPrompt, userPrompt), 0);
            throw ex;
        }
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.generateQuestions(prompt, params);
            logCall("question_generate", start, estimateTokens(prompt, ""), estimateTokens(result));
            return result;
        } catch (RuntimeException ex) {
            logCall("question_generate", start, estimateTokens(prompt, ""), 0);
            throw ex;
        }
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        assertQuota();
        long start = System.currentTimeMillis();
        delegate.streamChat(systemPrompt, userPrompt, new StreamCallback() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void onReasoning(String content) {
                callback.onReasoning(content);
            }

            @Override
            public void onChunk(String content) {
                buffer.append(content);
                callback.onChunk(content);
            }

            @Override
            public void onStatus(String phase, String message) {
                callback.onStatus(phase, message);
            }

            @Override
            public void onComplete() {
                logCall("chat_stream", start, estimateTokens(systemPrompt, userPrompt), estimateTokens(buffer.toString()));
                callback.onComplete();
            }

            @Override
            public void onError(String message) {
                logCall("chat_stream", start, estimateTokens(systemPrompt, userPrompt), 0);
                callback.onError(message);
            }
        });
    }

    private void assertQuota() {
        if (StpUtil.isLogin()) {
            if (!aiQuotaService.checkAndIncrementDailyQuota(StpUtil.getLoginIdAsLong(), dailyQuota)) {
                throw new BusinessException("今日 AI 调用次数已达上限");
            }
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && tenantId > 0 && tenantQuotaApi != null) {
            tenantQuotaApi.checkTokenQuotaAvailable(tenantId);
        }
    }

    private void logCall(String scene, long startMs, int promptTokens, int completionTokens) {
        AiCallLogEntity entity = new AiCallLogEntity();
        if (StpUtil.isLogin()) {
            entity.setUserId(StpUtil.getLoginIdAsLong());
        }
        entity.setModel(properties.getModel());
        entity.setScene(scene);
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setLatencyMs((int) (System.currentTimeMillis() - startMs));
        entity.setCreateTime(LocalDateTime.now());
        aiCallLogDao.insert(entity);

        // 原子扣减租户 Token 配额
        Long tenantId = entity.getTenantId() != null ? entity.getTenantId() : TenantContext.getTenantId();
        if (tenantId != null && tenantId > 0 && tenantQuotaApi != null) {
            long totalTokens = (long) promptTokens + completionTokens;
            if (totalTokens > 0) {
                tenantQuotaApi.consumeTokenQuota(tenantId, totalTokens);
            }
        }
    }

    private int estimateTokens(String... texts) {
        int length = 0;
        for (String text : texts) {
            if (text != null) {
                length += text.length();
            }
        }
        return Math.max(1, length / 4);
    }
}
