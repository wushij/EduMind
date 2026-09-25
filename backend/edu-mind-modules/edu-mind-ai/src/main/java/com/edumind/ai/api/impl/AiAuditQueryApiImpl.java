package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.ai.service.audit.AiAuditQueryService;
import com.edumind.ai.vo.audit.AiUsageSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAuditQueryApiImpl implements AiAuditQueryApi {

    private final AiAuditQueryService aiAuditQueryService;

    @Override
    public long countCallsByKnowledgeBases(List<Long> knowledgeBaseIds) {
        return aiAuditQueryService.countCallsByKnowledgeBases(knowledgeBaseIds);
    }

    @Override
    public long countTotalCalls() {
        return aiAuditQueryService.countTotalCalls();
    }

    @Override
    public AiUsageSummaryVO getUsageSummary(List<Long> knowledgeBaseIds, LocalDateTime since) {
        return aiAuditQueryService.getUsageSummary(null, knowledgeBaseIds, since);
    }

    @Override
    public AiUsageSummaryVO getUsageSummary(Long courseId, List<Long> knowledgeBaseIds, LocalDateTime since) {
        return aiAuditQueryService.getUsageSummary(courseId, knowledgeBaseIds, since);
    }

    @Override
    public com.edumind.ai.vo.audit.AiCallLogPageVO pageLogs(Long courseId, List<Long> knowledgeBaseIds, String scene, String model, LocalDateTime since, long pageNum, long pageSize) {
        return aiAuditQueryService.pageLogs(courseId, knowledgeBaseIds, scene, model, since, pageNum, pageSize);
    }

    @Override
    public long countCallsByCourse(Long courseId, LocalDateTime since) {
        return aiAuditQueryService.countCallsByCourse(courseId, since);
    }

    @Override
    public Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since) {
        return aiAuditQueryService.countCallsByCourseBatch(courseIds, since);
    }

    @Override
    public long countCallsByCourseAndUser(Long courseId, Long userId, LocalDateTime since) {
        return aiAuditQueryService.countCallsByCourseAndUser(courseId, userId, since);
    }

    @Override
    public Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since) {
        return aiAuditQueryService.countCallsByScene(courseId, since);
    }
}
