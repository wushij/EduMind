package com.edumind.statistics.service.analytics.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.ai.vo.audit.AiUsageSummaryVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.statistics.service.analytics.AiUsageAnalyticsService;
import com.edumind.statistics.vo.analytics.AiUsageAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiUsageAnalyticsServiceImpl implements AiUsageAnalyticsService {

    private final AiAuditQueryApi aiAuditQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;

    /**
     * ai_call_log.scene 场景码（大写归一）-> 教育业务场景展示口径。
     *
     * <p>场景码由各 AI 调用方落库时写入，历史存在大小写混用（CHAT / chat / question_generate），
     * 故此处 key 统一为大写。未收录的场景码统一归入 {@link #SCENE_FALLBACK}，
     * 保证所有真实调用都被统计，不会因出现新场景而丢失调用量。</p>
     */
    private static final Map<String, String> SCENE_BUCKETS = Map.ofEntries(
            // 智能答疑解惑：对话式问答 / RAG 检索增强问答 / 智能体
            Map.entry("CHAT", "智能答疑解惑"),
            Map.entry("CHAT_RAG", "智能答疑解惑"),
            Map.entry("AI_CHAT", "智能答疑解惑"),
            Map.entry("RAG", "智能答疑解惑"),
            Map.entry("KB_RETRIEVAL", "智能答疑解惑"),
            Map.entry("GLOBAL_ASSISTANT", "智能答疑解惑"),
            Map.entry("AGENT", "智能答疑解惑"),
            // 试题精准批阅：主观题 / 作业智能批改
            Map.entry("GRADING", "试题精准批阅"),
            Map.entry("SUBJECTIVE_GRADING", "试题精准批阅"),
            // 靶向变式推演：出题 / 变式 / 组卷
            Map.entry("QUESTION_GENERATE", "靶向变式推演"),
            Map.entry("QUESTION_GEN", "靶向变式推演"),
            Map.entry("QUESTION", "靶向变式推演"),
            Map.entry("EXAM_SWAP_QUESTION", "靶向变式推演"),
            Map.entry("PAPER_COMPOSE", "靶向变式推演"),
            Map.entry("EXAM", "靶向变式推演"),
            // 学情诊断评估：学情分析 / 教学建议 / 记忆抽取
            Map.entry("LEARNING", "学情诊断评估"),
            Map.entry("EVALUATION", "学情诊断评估"),
            Map.entry("TEACHING_ADVICE", "学情诊断评估"),
            Map.entry("MEMORY_EXTRACT", "学情诊断评估"),
            Map.entry("SUMMARY", "学情诊断评估")
    );

    /** 未收录场景码的兜底口径（教学工具类调用，如备课、大纲生成、图谱建议） */
    private static final String SCENE_FALLBACK = "教学备课辅助";

    @Override
    public AiUsageAnalyticsVO getUsage(Long courseId, String range) {
        LocalDateTime since = resolveSince(range);
        List<Long> kbIds = null;
        if (courseId != null) {
            kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                    .map(KnowledgeBaseVO::getId)
                    .collect(Collectors.toList());
        }

        AiUsageSummaryVO summary = aiAuditQueryApi.getUsageSummary(kbIds, since);

        AiUsageAnalyticsVO vo = new AiUsageAnalyticsVO();
        vo.setTotalCalls(summary.getTotalCalls());
        vo.setTotalTokens(summary.getTotalTokens());

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Map.Entry<LocalDate, Long> e : summary.getDailyCalls().entrySet()) {
            AiUsageAnalyticsVO.DailyUsageVO d = new AiUsageAnalyticsVO.DailyUsageVO();
            d.setDate(e.getKey().format(fmt));
            d.setCalls(e.getValue());
            d.setTokens(summary.getDailyTokens().getOrDefault(e.getKey(), 0L));
            vo.getDaily().add(d);
        }
        for (Map.Entry<String, Long> e : summary.getProviderCalls().entrySet()) {
            AiUsageAnalyticsVO.ProviderUsageVO p = new AiUsageAnalyticsVO.ProviderUsageVO();
            p.setProvider(e.getKey());
            p.setCalls(e.getValue());
            p.setTokens(summary.getProviderTokens().getOrDefault(e.getKey(), 0L));
            vo.getByProvider().add(p);
        }

        // 场景分布：直接读取 ai_call_log.scene 的真实分组并归并口径，不使用任何静态比例
        vo.setByScene(buildSceneUsage(courseId, since));
        return vo;
    }

    /**
     * 按真实调用场景构建教育业务场景分布。
     *
     * <p>课程维度优先直接按 course_id 统计（与"AI 助学调用次数"KPI 口径保持一致）；
     * 课程维度无数据时回退为全局分布，确保环形图在课程上下文缺失时仍有真实依据。</p>
     */
    private List<AiUsageAnalyticsVO.SceneUsageVO> buildSceneUsage(Long courseId, LocalDateTime since) {
        Map<String, Long> sceneCalls = aiAuditQueryApi.countCallsByScene(courseId, since);
        if (sceneCalls.isEmpty() && courseId != null) {
            sceneCalls = aiAuditQueryApi.countCallsByScene(null, since);
        }
        if (sceneCalls.isEmpty()) {
            return Collections.emptyList();
        }

        // 归并到教育业务场景，并保留来源场景码便于核对口径
        Map<String, Long> bucketCalls = new LinkedHashMap<>();
        Map<String, List<String>> bucketSources = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : sceneCalls.entrySet()) {
            String bucket = SCENE_BUCKETS.getOrDefault(e.getKey(), SCENE_FALLBACK);
            bucketCalls.merge(bucket, e.getValue(), Long::sum);
            bucketSources.computeIfAbsent(bucket, k -> new ArrayList<>()).add(e.getKey());
        }

        long total = bucketCalls.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return Collections.emptyList();
        }

        return bucketCalls.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> {
                    AiUsageAnalyticsVO.SceneUsageVO item = new AiUsageAnalyticsVO.SceneUsageVO();
                    item.setScene(e.getKey());
                    item.setCalls(e.getValue());
                    item.setRatio(Math.round(e.getValue() * 1000.0 / total) / 10.0);
                    item.setSourceScenes(bucketSources.getOrDefault(e.getKey(), Collections.emptyList()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime resolveSince(String range) {
        if ("90d".equals(range) || "term".equals(range)) {
            return LocalDateTime.now().minusDays(90);
        }
        if ("30d".equals(range)) {
            return LocalDateTime.now().minusDays(30);
        }
        if ("7d".equals(range)) {
            return LocalDateTime.now().minusDays(7);
        }
        return LocalDateTime.now().minusDays(30);
    }
}
