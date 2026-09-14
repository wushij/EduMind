package com.edumind.ai.service.audit.impl;

import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.audit.AiCallAuditService;
import com.edumind.common.context.TenantContext;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.api.TenantQuotaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallAuditServiceImpl implements AiCallAuditService {

    private final AiCallLogDao aiCallLogDao;
    private final AiModelConfigDao aiModelConfigDao;
    private final TenantQuotaApi tenantQuotaApi;

    @Override
    public void record(String scene, String modelKey, AiCallAuditContext context, long startMs,
                       int promptTokens, int completionTokens) {
        try {
            Long userId = resolveUserId(context);
            Long tenantId = resolveTenantId(context);
            if (userId == null) {
                log.warn("Skip ai_call_log because userId is missing, scene={}", scene);
                return;
            }
            if (tenantId == null || tenantId <= 0) {
                log.warn("Skip ai_call_log because tenantId is missing, scene={} userId={}", scene, userId);
                return;
            }

            AiCallLogEntity entity = new AiCallLogEntity();
            entity.setUserId(userId);
            entity.setTenantId(tenantId);
            if (context != null) {
                entity.setCourseId(context.getCourseId());
                entity.setConversationId(context.getConversationId());
                entity.setKnowledgeBaseId(context.getKnowledgeBaseId());
                entity.setRetrievalHitCount(context.getRetrievalHitCount());
            }
            entity.setModel(resolveModelDisplay(modelKey));
            entity.setScene(normalizeScene(scene));
            entity.setPromptTokens(Math.max(0, promptTokens));
            entity.setCompletionTokens(Math.max(0, completionTokens));
            entity.setLatencyMs((int) Math.max(0, System.currentTimeMillis() - startMs));
            entity.setCreateTime(LocalDateTime.now());
            aiCallLogDao.insert(entity);
            consumeTenantQuota(tenantId, promptTokens, completionTokens);
        } catch (Exception ex) {
            log.warn("Failed to record ai_call_log scene={} modelKey={}: {}", scene, modelKey, ex.getMessage());
        }
    }

    @Override
    public void recordEstimated(String scene, String modelKey, AiCallAuditContext context, long startMs,
                                String promptText, String completionText) {
        record(scene, modelKey, context, startMs, estimateTokens(promptText), estimateTokens(completionText));
    }

    @Override
    public String resolveModelDisplay(String modelKey) {
        AiModelConfigEntity config = findConfig(modelKey);
        if (config == null) {
            return StringUtils.hasText(modelKey) ? modelKey : "unknown";
        }
        if (StringUtils.hasText(config.getModelName())) {
            return config.getModelName();
        }
        if (StringUtils.hasText(config.getModelKey())) {
            return config.getModelKey();
        }
        return StringUtils.hasText(config.getConfigName()) ? config.getConfigName() : "unknown";
    }

    private Long resolveUserId(AiCallAuditContext context) {
        if (context != null && context.getUserId() != null) {
            return context.getUserId();
        }
        return LoginUserResolver.resolveUserId();
    }

    private Long resolveTenantId(AiCallAuditContext context) {
        if (context != null && context.getTenantId() != null && context.getTenantId() > 0) {
            return context.getTenantId();
        }
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null && tenantId > 0 ? tenantId : null;
    }

    private AiModelConfigEntity findConfig(String modelKey) {
        if (!StringUtils.hasText(modelKey)) {
            return aiModelConfigDao.findDefaultByType("chat");
        }
        AiModelConfigEntity byKey = aiModelConfigDao.findByModelKey(modelKey);
        if (byKey != null) {
            return byKey;
        }
        return aiModelConfigDao.findByConfigName(modelKey);
    }

    private String normalizeScene(String scene) {
        return StringUtils.hasText(scene) ? scene.trim() : "unknown";
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private void consumeTenantQuota(Long tenantId, int promptTokens, int completionTokens) {
        if (tenantId == null || tenantId <= 0 || tenantQuotaApi == null) {
            return;
        }
        long totalTokens = (long) promptTokens + completionTokens;
        if (totalTokens > 0) {
            tenantQuotaApi.consumeTokenQuota(tenantId, totalTokens);
        }
    }
}
