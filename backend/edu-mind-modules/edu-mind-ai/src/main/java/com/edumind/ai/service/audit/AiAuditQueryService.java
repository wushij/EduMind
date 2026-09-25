package com.edumind.ai.service.audit;

import com.edumind.ai.vo.audit.AiUsageSummaryVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AiAuditQueryService {

    long countCallsByKnowledgeBases(List<Long> knowledgeBaseIds);

    long countTotalCalls();

    AiUsageSummaryVO getUsageSummary(List<Long> knowledgeBaseIds, LocalDateTime since);

    AiUsageSummaryVO getUsageSummary(Long courseId, List<Long> knowledgeBaseIds, LocalDateTime since);

    com.edumind.ai.vo.audit.AiCallLogPageVO pageLogs(Long courseId, List<Long> knowledgeBaseIds, String scene, String model, LocalDateTime since, long pageNum, long pageSize);

    long countCallsByCourse(Long courseId, LocalDateTime since);

    Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since);

    long countCallsByCourseAndUser(Long courseId, Long userId, LocalDateTime since);

    /**
     * 按调用场景（scene）分组统计调用量，scene 已做大写归一。
     */
    Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since);
}
