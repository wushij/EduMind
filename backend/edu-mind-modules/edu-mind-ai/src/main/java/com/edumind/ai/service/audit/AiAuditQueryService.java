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
     * 按课程与用户批量统计 AI 调用量（单次 GROUP BY 聚合）。
     *
     * <p>供学情榜单等"课程 × 全部学生"场景使用，替代逐学生调用
     * {@link #countCallsByCourseAndUser(Long, Long, LocalDateTime)} 造成的 N+1 查询。</p>
     *
     * @param courseId 课程 ID，为 null 表示不限定课程
     * @param userIds  用户 ID 集合
     * @param since    起始时间，可为 null
     * @return userId -> 调用次数（无调用的用户不在结果中，由调用方取 0）
     */
    Map<Long, Long> countCallsByCourseUserBatch(Long courseId, List<Long> userIds, LocalDateTime since);

    /**
     * 按调用场景（scene）分组统计调用量，scene 已做大写归一。
     */
    Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since);
}
