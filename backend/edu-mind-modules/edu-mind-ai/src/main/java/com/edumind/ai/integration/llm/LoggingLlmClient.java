package com.edumind.ai.integration.llm;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.ai.service.audit.AiCallAuditService;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.ai.service.quota.AiDailyQuotaService;
import com.edumind.system.api.TenantQuotaApi;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class LoggingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final AiCallAuditService aiCallAuditService;
    private final AiDailyQuotaService aiDailyQuotaService;
    private final long dailyQuota;
    private final TenantQuotaApi tenantQuotaApi;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.chat(systemPrompt, userPrompt);
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinPrompt(systemPrompt, userPrompt), result);
            return result;
        } catch (RuntimeException ex) {
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinPrompt(systemPrompt, userPrompt), "");
            throw ex;
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, LlmChatOptions options) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.chat(systemPrompt, userPrompt, options);
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinPrompt(systemPrompt, userPrompt), result);
            return result;
        } catch (RuntimeException ex) {
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinPrompt(systemPrompt, userPrompt), "");
            throw ex;
        }
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.generateQuestions(prompt, params);
            aiCallAuditService.recordEstimated("question_generate", null, null, start, prompt, result);
            return result;
        } catch (RuntimeException ex) {
            aiCallAuditService.recordEstimated("question_generate", null, null, start, prompt, "");
            throw ex;
        }
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, List.of(LlmChatMessage.user(userPrompt)), callback);
    }

    @Override
    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages, StreamCallback callback) {
        assertQuota();
        long start = System.currentTimeMillis();
        delegate.streamChatWithHistory(systemPrompt, messages, new StreamCallback() {
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
                aiCallAuditService.recordEstimated("CHAT", null, null, start,
                        joinHistoryPrompt(systemPrompt, messages), buffer.toString());
                callback.onComplete();
            }

            @Override
            public void onError(String message) {
                aiCallAuditService.recordEstimated("CHAT", null, null, start,
                        joinHistoryPrompt(systemPrompt, messages), buffer.toString());
                callback.onError(message);
            }
        });
    }

    @Override
    public String chatWithHistory(String systemPrompt, List<LlmChatMessage> messages) {
        assertQuota();
        long start = System.currentTimeMillis();
        try {
            String result = delegate.chatWithHistory(systemPrompt, messages);
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinHistoryPrompt(systemPrompt, messages), result);
            return result;
        } catch (RuntimeException ex) {
            aiCallAuditService.recordEstimated("CHAT", null, null, start,
                    joinHistoryPrompt(systemPrompt, messages), "");
            throw ex;
        }
    }

    private void assertQuota() {
        if (StpUtil.isLogin()) {
            if (!aiDailyQuotaService.checkAndIncrementDailyQuota(StpUtil.getLoginIdAsLong(), dailyQuota)) {
                throw new BusinessException("今日 AI 调用次数已达上限");
            }
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && tenantId > 0 && tenantQuotaApi != null) {
            tenantQuotaApi.checkTokenQuotaAvailable(tenantId);
        }
    }

    private String joinPrompt(String systemPrompt, String userPrompt) {
        return (systemPrompt != null ? systemPrompt : "") + "\n" + (userPrompt != null ? userPrompt : "");
    }

    private String joinHistoryPrompt(String systemPrompt, List<LlmChatMessage> messages) {
        StringBuilder builder = new StringBuilder(systemPrompt != null ? systemPrompt : "");
        if (messages != null) {
            for (LlmChatMessage message : messages) {
                if (message != null && message.getContent() != null) {
                    builder.append('\n').append(message.getContent());
                }
            }
        }
        return builder.toString();
    }
}
