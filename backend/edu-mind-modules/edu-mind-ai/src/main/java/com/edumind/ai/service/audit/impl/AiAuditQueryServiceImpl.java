package com.edumind.ai.service.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.service.audit.AiAuditQueryService;
import com.edumind.ai.vo.audit.AiCallLogPageVO;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.audit.AiUsageSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAuditQueryServiceImpl implements AiAuditQueryService {

    private final AiCallLogDao aiCallLogDao;
    private final AiConverter aiConverter;
    private final UserQueryApi userQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;

    private static final Map<String, String> SCENE_LABELS = Map.ofEntries(
            Map.entry("chat", "AI 智能对话"),
            Map.entry("chat_stream", "AI 智能对话"),
            Map.entry("CHAT", "课程 AI 智能对话"),
            Map.entry("CHAT_RAG", "课程 AI 智能助教 (RAG 问答)"),
            Map.entry("CHAT_TITLE", "会话标题生成"),
            Map.entry("chat_title", "会话标题生成"),
            Map.entry("global_assistant", "全局 AI 教学助手"),
            Map.entry("GLOBAL_ASSISTANT", "全局 AI 教学助手"),
            Map.entry("learning", "AI 自适应学习辅导"),
            Map.entry("LEARNING", "AI 自适应学习辅导"),
            Map.entry("memory_extract", "学情长期记忆沉淀"),
            Map.entry("MEMORY_EXTRACT", "学情长期记忆沉淀"),
            Map.entry("kb_retrieval", "知识库检索增强"),
            Map.entry("KB_RETRIEVAL", "知识库检索增强"),
            Map.entry("evaluation", "学情综合诊断评估"),
            Map.entry("EVALUATION", "学情综合诊断评估"),
            Map.entry("question", "AI 自适应试题生成"),
            Map.entry("QUESTION", "AI 自适应试题生成"),
            Map.entry("question_generate", "AI 自适应练习生成"),
            Map.entry("QUESTION_GENERATE", "AI 自适应练习生成"),
            Map.entry("subjective_grading", "主观题智能评阅与批改"),
            Map.entry("SUBJECTIVE_GRADING", "主观题智能评阅与批改"),
            Map.entry("grading", "主观题智能评阅与批改"),
            Map.entry("GRADING", "主观题智能评阅与批改"),
            Map.entry("summary", "章节摘要提炼"),
            Map.entry("SUMMARY", "章节摘要提炼"),
            Map.entry("lesson_plan", "智能教案生成"),
            Map.entry("LESSON_PLAN", "智能教案生成"),
            Map.entry("prep", "智能备课教案"),
            Map.entry("PREP", "智能备课教案"),
            // 以下场景原先未收录，会原样展示 "TEACHING_ADVICE" 这类内部代号。
            // 它们（课程级调用补上 course_id 后）会出现在课程的调用明细里，必须给出中文口径。
            Map.entry("CHAT_FOLLOW_UP", "追问建议生成"),
            Map.entry("chat_follow_up", "追问建议生成"),
            Map.entry("TEACHING_ADVICE", "AI 学情诊断与教学建议"),
            Map.entry("TEACHING_INTERVENTION", "教学精准干预推演"),
            Map.entry("ANALYTICS", "全班错因宏观研判"),
            Map.entry("COURSE_OBJECTIVE", "课程教学目标 AI 推荐"),
            Map.entry("COURSE_KNOWLEDGE_POINT", "课程知识点 AI 推荐"),
            Map.entry("COURSE_DESCRIPTION", "课程简介 AI 生成"),
            Map.entry("GRAPH_SUGGEST", "知识图谱关系推荐"),
            Map.entry("AGENT", "AI Agent 任务规划"),
            Map.entry("RAG", "RAG 知识检索问答")
    );

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
        return getUsageSummary(null, knowledgeBaseIds, since);
    }

    @Override
    public AiUsageSummaryVO getUsageSummary(Long courseId, List<Long> knowledgeBaseIds, LocalDateTime since) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = buildScopeWrapper(courseId, knowledgeBaseIds, since);

        List<AiCallLogEntity> logs = aiCallLogDao.list(wrapper);
        AiUsageSummaryVO summary = new AiUsageSummaryVO();
        summary.setTotalCalls((long) logs.size());

        long tokens = 0L;
        long totalLatency = 0L;
        long todayCalls = 0L;
        long todayTokens = 0L;
        long successfulCalls = 0L;
        LocalDate today = LocalDate.now();

        Map<LocalDate, Long> dailyCalls = new HashMap<>();
        Map<LocalDate, Long> dailyTokens = new HashMap<>();
        Map<String, Long> providerCalls = new HashMap<>();
        Map<String, Long> providerTokens = new HashMap<>();

        for (AiCallLogEntity log : logs) {
            long pTok = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
            long cTok = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;
            long t = pTok + cTok;
            tokens += t;

            if (log.getLatencyMs() != null && log.getLatencyMs() > 0) {
                totalLatency += log.getLatencyMs();
            }
            successfulCalls++;

            if (log.getCreateTime() != null) {
                LocalDate day = log.getCreateTime().toLocalDate();
                dailyCalls.merge(day, 1L, Long::sum);
                dailyTokens.merge(day, t, Long::sum);

                if (day.equals(today)) {
                    todayCalls++;
                    todayTokens += t;
                }
            }
            String provider = StringUtils.hasText(log.getModel()) ? log.getModel().trim() : "unknown";
            providerCalls.merge(provider, 1L, Long::sum);
            providerTokens.merge(provider, t, Long::sum);
        }

        summary.setTotalTokens(tokens);
        summary.setTotalLatencyMs(totalLatency);
        summary.setTodayCalls(todayCalls);
        summary.setTodayTokens(todayTokens);
        summary.setSuccessfulCalls(successfulCalls);
        summary.setDailyCalls(dailyCalls);
        summary.setDailyTokens(dailyTokens);
        summary.setProviderCalls(providerCalls);
        summary.setProviderTokens(providerTokens);
        return summary;
    }

    @Override
    public AiCallLogPageVO pageLogs(Long courseId, List<Long> knowledgeBaseIds, String scene, String model,
                                    LocalDateTime since, long pageNum, long pageSize) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = buildScopeWrapper(courseId, knowledgeBaseIds, since);

        if (StringUtils.hasText(scene)) {
            wrapper.eq(AiCallLogEntity::getScene, scene.trim());
        }
        if (StringUtils.hasText(model)) {
            wrapper.like(AiCallLogEntity::getModel, model.trim());
        }

        wrapper.orderByDesc(AiCallLogEntity::getCreateTime);

        Page<AiCallLogEntity> pageResult = aiCallLogDao.page(new Page<>(pageNum, pageSize), wrapper);
        List<AiCallLogEntity> records = pageResult.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return AiCallLogPageVO.builder()
                    .list(Collections.emptyList())
                    .total(pageResult.getTotal())
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .build();
        }

        // 1. 批量填充用户明细（调用人姓名、用户名、头像、角色）
        Set<Long> userIds = records.stream()
                .map(AiCallLogEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserBriefVO> userBriefMap = (userQueryApi != null && !userIds.isEmpty())
                ? userQueryApi.mapUserBriefsByIds(userIds)
                : Collections.emptyMap();
        Map<Long, List<String>> roleCodesMap = (userQueryApi != null && !userIds.isEmpty())
                ? userQueryApi.mapRoleCodesByUserIds(userIds)
                : Collections.emptyMap();

        // 2. 批量填充课程名称
        Set<Long> courseIds = records.stream()
                .map(AiCallLogEntity::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> courseNameMap = new HashMap<>();
        if (courseQueryApi != null && !courseIds.isEmpty()) {
            try {
                courseQueryApi.listCoursesByIds(new ArrayList<>(courseIds)).forEach(c -> {
                    if (c != null && c.getId() != null && StringUtils.hasText(c.getName())) {
                        courseNameMap.put(c.getId(), c.getName());
                    }
                });
            } catch (Exception e) {
                // 忽略外部跨模块查询容错
            }
        }

        // 3. 批量填充知识库名称
        Set<Long> kbIds = records.stream()
                .map(AiCallLogEntity::getKnowledgeBaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> kbNameMap = new HashMap<>();
        if (knowledgeQueryApi != null && !kbIds.isEmpty()) {
            for (Long kbId : kbIds) {
                try {
                    KnowledgeBaseVO kb = knowledgeQueryApi.getKnowledgeBaseById(kbId);
                    if (kb != null && StringUtils.hasText(kb.getName())) {
                        kbNameMap.put(kbId, kb.getName());
                    }
                } catch (Exception e) {
                    // 忽略外部跨模块查询容错
                }
            }
        }

        List<AiCallLogVO> voList = records.stream()
                .map(entity -> {
                    AiCallLogVO vo = aiConverter.toCallLogVO(entity);
                    vo.setSceneLabel(resolveSceneLabel(vo.getScene()));
                    if (entity.getUserId() != null) {
                        UserBriefVO brief = userBriefMap.get(entity.getUserId());
                        if (brief != null) {
                            vo.setUsername(brief.getUsername());
                            vo.setRealName(brief.getRealName());
                            vo.setAvatar(brief.getAvatar());
                        }
                        List<String> roles = roleCodesMap.get(entity.getUserId());
                        if (!CollectionUtils.isEmpty(roles)) {
                            vo.setUserRole(roles.get(0));
                        }
                    }
                    if (entity.getCourseId() != null) {
                        vo.setCourseName(courseNameMap.get(entity.getCourseId()));
                    }
                    if (entity.getKnowledgeBaseId() != null) {
                        vo.setKnowledgeBaseName(kbNameMap.get(entity.getKnowledgeBaseId()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return AiCallLogPageVO.builder()
                .list(voList)
                .total(pageResult.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
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
    public long countCallsByCourseAndUser(Long courseId, Long userId, LocalDateTime since) {
        if (courseId == null || userId == null) {
            return 0L;
        }
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getCourseId, courseId)
                .eq(AiCallLogEntity::getUserId, userId)
                .ge(since != null, AiCallLogEntity::getCreateTime, since);
        return aiCallLogDao.count(wrapper);
    }

    @Override
    public Map<Long, Long> countCallsByCourseUserBatch(Long courseId, List<Long> userIds, LocalDateTime since) {
        if (courseId == null || CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return aiCallLogDao.countGroupByUserIds(courseId, userIds, since);
    }

    @Override
    public Map<Long, Long> countCallsByCourseBatch(List<Long> courseIds, LocalDateTime since) {
        Map<Long, Long> result = new HashMap<>();
        if (CollectionUtils.isEmpty(courseIds)) {
            return result;
        }
        Map<Long, Long> groupedCounts = aiCallLogDao.countGroupByCourseIds(courseIds, since);
        for (Long courseId : courseIds) {
            result.put(courseId, groupedCounts.getOrDefault(courseId, 0L));
        }
        return result;
    }

    @Override
    public Map<String, Long> countCallsByScene(Long courseId, LocalDateTime since) {
        return aiCallLogDao.countGroupByScene(courseId, since);
    }

    /**
     * 构建涵盖课程 ID 与其关联知识库的组合范围查询条件。
     * 当指定 courseId 时，统筹 course_id = courseId OR knowledge_base_id IN (kbIds)，杜绝漏统。
     */
    private LambdaQueryWrapper<AiCallLogEntity> buildScopeWrapper(Long courseId, List<Long> knowledgeBaseIds, LocalDateTime since) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(since != null, AiCallLogEntity::getCreateTime, since);

        if (courseId != null) {
            wrapper.and(w -> {
                w.eq(AiCallLogEntity::getCourseId, courseId);
                if (!CollectionUtils.isEmpty(knowledgeBaseIds)) {
                    w.or().in(AiCallLogEntity::getKnowledgeBaseId, knowledgeBaseIds);
                }
            });
        } else if (!CollectionUtils.isEmpty(knowledgeBaseIds)) {
            wrapper.in(AiCallLogEntity::getKnowledgeBaseId, knowledgeBaseIds);
        }

        return wrapper;
    }

    private String resolveSceneLabel(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "AI 综合能力";
        }
        String label = SCENE_LABELS.get(scene);
        if (label != null) {
            return label;
        }
        label = SCENE_LABELS.get(scene.toUpperCase());
        if (label != null) {
            return label;
        }
        label = SCENE_LABELS.get(scene.toLowerCase());
        return label != null ? label : scene;
    }
}
