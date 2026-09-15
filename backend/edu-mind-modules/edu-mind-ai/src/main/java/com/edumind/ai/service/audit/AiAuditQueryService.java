package com.edumind.ai.service.audit;

import com.edumind.ai.vo.audit.AiUsageSummaryVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AiAuditQueryService {

    long countCallsByKnowledgeBases(List<Long> knowledgeBaseIds);

    long countTotalCalls();

    AiUsageSummaryVO getUsageSummary(List<Long> knowledgeBaseIds, LocalDateTime since);

    long countCallsByCourse(Long courseId, LocalDateTime since);

    Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since);
}
