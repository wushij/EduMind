package com.edumind.ai.service.memory.retrieval;

import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.service.memory.MemoryCryptoService;
import com.edumind.common.context.TenantContext;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 长期记忆检索管线服务 (支持知情同意前置校验、过期过滤、文本加权打分与敏感解密)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryRetrievalService {

    private final AiMemoryDao aiMemoryDao;
    private final MemoryCryptoService memoryCryptoService;

    /**
     * 自动从当前上下文解析租户与用户进行记忆检索
     */
    public List<MemoryContextBlock> retrieve(Long courseId, String queryPrompt, int topK) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();
        return retrieve(tenantId, userId, courseId, queryPrompt, topK);
    }

    /**
     * 执行长期记忆检索管线
     */
    public List<MemoryContextBlock> retrieve(Long tenantId, Long userId, Long courseId, String queryPrompt, int topK) {
        if (tenantId == null || userId == null) {
            log.debug("[长期记忆检索] 租户或用户未识别，Fail-Closed 返回空");
            return Collections.emptyList();
        }

        // 1. Consent 前置隐私鉴权检查：未授权严格拒绝检索，避免任何记忆泄露
        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            log.debug("[长期记忆检索] 租户 {} 用户 {} 课程 {} 记忆空间未获知情同意，跳过召回", tenantId, userId, courseId);
            return Collections.emptyList();
        }

        // 2. 获取该空间下的所有条目
        List<AiMemoryItemEntity> allItems = aiMemoryDao.listItemsByNamespace(namespace.getId());
        if (allItems == null || allItems.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        // 3. 生命周期过滤：忽略已过期的记忆
        List<AiMemoryItemEntity> validItems = allItems.stream()
                .filter(item -> item.getExpireTime() == null || item.getExpireTime().isAfter(now))
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Beta 相关度评分与排序 (关键词多命中加权 + 时效性)
        final String queryNormalized = queryPrompt != null ? queryPrompt.toLowerCase().trim() : "";
        final String[] queryKeywords = queryNormalized.isEmpty() ? new String[0] : queryNormalized.split("\\s+");

        List<MemoryScoredItem> scoredItems = validItems.stream()
                .map(item -> {
                    double score = computeScore(item, queryKeywords);
                    return new MemoryScoredItem(item, score);
                })
                .sorted(Comparator.comparingDouble(MemoryScoredItem::score).reversed()
                        .thenComparing((MemoryScoredItem m) -> m.item().getCreateTime() != null ? m.item().getCreateTime() : LocalDateTime.MIN, Comparator.reverseOrder()))
                .limit(topK > 0 ? topK : 5)
                .collect(Collectors.toList());

        // 5. 组装 MemoryContextBlock 并按需解密 SM4 敏感信息
        return scoredItems.stream().map(scored -> {
            AiMemoryItemEntity entity = scored.item();
            String decryptedContent = null;
            if (StringUtils.hasText(entity.getContentCiphertext())) {
                decryptedContent = memoryCryptoService.decrypt(tenantId, entity.getContentCiphertext());
            }

            return MemoryContextBlock.builder()
                    .id(entity.getId())
                    .summary(entity.getSummary())
                    .fullContent(decryptedContent)
                    .memoryType(StringUtils.hasText(entity.getMemoryType()) ? entity.getMemoryType() : "PREFERENCE")
                    .sensitivityLevel(entity.getSensitivityLevel())
                    .score(Math.round(scored.score() * 100.0) / 100.0)
                    .encrypted(StringUtils.hasText(entity.getContentCiphertext()))
                    .build();
        }).collect(Collectors.toList());
    }

    private double computeScore(AiMemoryItemEntity item, String[] queryKeywords) {
        if (queryKeywords.length == 0) {
            return 0.85; // 默认基础置信分
        }
        String targetText = (item.getSummary() != null ? item.getSummary() : "").toLowerCase();
        int matches = 0;
        for (String kw : queryKeywords) {
            if (!kw.isBlank() && targetText.contains(kw)) {
                matches++;
            }
        }
        if (matches == 0) {
            return 0.50; // 无直接关键词匹配但同空间保留基础分
        }
        // 根据关键词匹配比例放大至 0.6 ~ 0.99
        double ratio = (double) matches / queryKeywords.length;
        return Math.min(0.99, 0.60 + ratio * 0.39);
    }

    private record MemoryScoredItem(AiMemoryItemEntity item, double score) {}
}
