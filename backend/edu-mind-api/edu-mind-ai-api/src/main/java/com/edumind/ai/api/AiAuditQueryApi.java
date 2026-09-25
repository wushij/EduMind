package com.edumind.ai.api;

import com.edumind.ai.vo.audit.AiUsageSummaryVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 审计与调用日志跨模块公开查询 API
 */
public interface AiAuditQueryApi {

    /**
     * 按知识库 ID 列表统计 AI 调用总次数
     *
     * @param knowledgeBaseIds 知识库 ID 集合（若为空集合则统计 0）
     * @return 调用总次数
     */
    long countCallsByKnowledgeBases(List<Long> knowledgeBaseIds);

    /**
     * 统计全局 AI 调用总次数
     *
     * @return 调用总次数
     */
    long countTotalCalls();

    /**
     * 按知识库与时间范围查询 AI 用量聚合摘要（兼容旧口径）
     *
     * @param knowledgeBaseIds 知识库 ID 集合（为 null 表示不限制知识库；为空集合表示过滤无结果）
     * @param since            起始时间（可为 null）
     * @return 聚合统计摘要
     */
    AiUsageSummaryVO getUsageSummary(List<Long> knowledgeBaseIds, LocalDateTime since);

    /**
     * 按课程 ID、知识库 ID 列表及时间范围查询 AI 用量聚合摘要。
     * 具备双重联查能力：同时统筹 course_id = courseId 与 knowledge_base_id IN (knowledgeBaseIds)，
     * 避免课程未绑定知识库或调用未带知识库 ID 时的漏统。
     *
     * @param courseId         课程 ID（可为 null，表示全校/全局统计）
     * @param knowledgeBaseIds 该课程关联的知识库 ID 集合（可为 null 或空）
     * @param since            起始时间（可为 null）
     * @return 聚合统计摘要
     */
    AiUsageSummaryVO getUsageSummary(Long courseId, List<Long> knowledgeBaseIds, LocalDateTime since);

    /**
     * 分页查询 AI 调用明细日志
     *
     * @param courseId         课程 ID（可为 null）
     * @param knowledgeBaseIds 知识库 ID 集合（可为 null）
     * @param scene            场景码过滤（可为 null）
     * @param model            模型关键字过滤（可为 null）
     * @param since            起始时间（可为 null）
     * @param pageNum          当前页码（从 1 开始）
     * @param pageSize         每页条数
     * @return 分页结果
     */
    com.edumind.ai.vo.audit.AiCallLogPageVO pageLogs(Long courseId, List<Long> knowledgeBaseIds, String scene, String model, LocalDateTime since, long pageNum, long pageSize);

    /**
     * 按课程 ID 及起始时间统计 AI 调用量
     */
    long countCallsByCourse(Long courseId, LocalDateTime since);

    /**
     * 按课程 ID 列表批量统计 AI 调用量
     */
    Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since);

    /**
     * 按课程、用户及起始时间统计 AI 调用量
     */
    long countCallsByCourseAndUser(Long courseId, Long userId, LocalDateTime since);

    /**
     * 按调用场景（ai_call_log.scene）分组统计调用量。
     *
     * <p>返回的是场景码归一后（大写）的原始事实，场景码到展示口径的映射由调用方
     * 按自身分析诉求处理，避免 AI 模块耦合具体报表口径。</p>
     *
     * @param courseId 课程 ID，为 null 表示全局统计
     * @param since    起始时间，可为 null
     * @return 场景码（大写） -> 调用次数
     */
    Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since);
}
