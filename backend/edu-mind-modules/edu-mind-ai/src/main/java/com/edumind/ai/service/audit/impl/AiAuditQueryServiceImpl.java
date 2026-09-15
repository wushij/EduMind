package com.edumind.ai.service.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.service.audit.AiAuditQueryService;
import com.edumind.ai.vo.audit.AiUsageSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAuditQueryServiceImpl implements AiAuditQueryService {

    private final AiCallLogDao aiCallLogDao;

    @Override
    public long countCallsByKnowledgeBases(List<Long> knowledgeBaseIds) {
        if (CollectionUtils.isEmpty(knowledgeBaseIds)) {
            return 0L;
        }
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .in(AiCallLogEntity::getKnowledgeBaseId, knowledgeBaseIds);
        return aiCallLogDao.count(wrapper);
    }

    @Override
    public long countTotalCalls() {
        return aiCallLogDao.count(new LambdaQueryWrapper<>());
    }

    @Override
    public AiUsageSummaryVO getUsageSummary(List<Long> knowledgeBaseIds, LocalDateTime since) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(since != null, AiCallLogEntity::getCreateTime, since);

        if (knowledgeBaseIds != null) {
            if (knowledgeBaseIds.isEmpty()) {
                return new AiUsageSummaryVO();
            }
            wrapper.in(AiCallLogEntity::getKnowledgeBaseId, knowledgeBaseIds);
        }

        List<AiCallLogEntity> logs = aiCallLogDao.list(wrapper);
        AiUsageSummaryVO summary = new AiUsageSummaryVO();
        summary.setTotalCalls((long) logs.size());

        long tokens = logs.stream().mapToLong(l ->
                (l.getPromptTokens() != null ? l.getPromptTokens() : 0)
                        + (l.getCompletionTokens() != null ? l.getCompletionTokens() : 0)).sum();
        summary.setTotalTokens(tokens);

        Map<LocalDate, Long> dailyCalls = new HashMap<>();
        Map<LocalDate, Long> dailyTokens = new HashMap<>();
        Map<String, Long> providerCalls = new HashMap<>();
        Map<String, Long> providerTokens = new HashMap<>();

        for (AiCallLogEntity log : logs) {
            if (log.getCreateTime() != null) {
                LocalDate day = log.getCreateTime().toLocalDate();
                dailyCalls.merge(day, 1L, Long::sum);
                long t = (log.getPromptTokens() != null ? log.getPromptTokens() : 0)
                        + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0);
                dailyTokens.merge(day, t, Long::sum);
            }
            String provider = log.getModel() != null ? log.getModel() : "unknown";
            long t = (log.getPromptTokens() != null ? log.getPromptTokens() : 0)
                    + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0);
            providerCalls.merge(provider, 1L, Long::sum);
            providerTokens.merge(provider, t, Long::sum);
        }

        summary.setDailyCalls(dailyCalls);
        summary.setDailyTokens(dailyTokens);
        summary.setProviderCalls(providerCalls);
        summary.setProviderTokens(providerTokens);
        return summary;
    }

    @Override
    public long countCallsByCourse(Long courseId, LocalDateTime since) {
        if (courseId == null) {
            return 0L;
        }
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getCourseId, courseId)
                .ge(since != null, AiCallLogEntity::getCreateTime, since);
        return aiCallLogDao.count(wrapper);
    }

    @Override
    public Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since) {
        Map<Long, Long> result = new HashMap<>();
        if (CollectionUtils.isEmpty(courseIds)) {
            return result;
        }
        for (Long cid : courseIds) {
            result.put(cid, countCallsByCourse(cid, since));
        }
        return result;
    }
}
