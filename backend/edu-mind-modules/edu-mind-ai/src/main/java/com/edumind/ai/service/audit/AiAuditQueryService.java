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

    long countCallsByCourseAndUser(Long courseId, Long userId, LocalDateTime since);

    /**
     * 按调用场景（scene）分组统计调用量，scene 已做大写归一。
     */
    Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since);
}
