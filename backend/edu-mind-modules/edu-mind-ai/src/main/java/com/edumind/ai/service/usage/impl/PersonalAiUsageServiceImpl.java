package com.edumind.ai.service.usage.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.SysAiQuotaDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.SysAiQuotaEntity;
import com.edumind.ai.service.usage.PersonalAiUsageService;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.usage.PersonalAiUsageLogVO;
import com.edumind.ai.vo.usage.PersonalAiUsageVO;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.api.SysConfigQueryApi;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalAiUsageServiceImpl implements PersonalAiUsageService {

    private static final Set<String> QA_AND_GENERATE_SCENES = Set.of(
            "chat", "chat_stream", "CHAT", "CHAT_RAG",
            "global_assistant", "GLOBAL_ASSISTANT",
            "question_generate", "subjective_grading"
    );

    private static final Map<String, String> SCENE_LABELS = Map.ofEntries(
            Map.entry("chat", "AI 智能对话"),
            Map.entry("chat_stream", "AI 智能对话"),
            Map.entry("CHAT", "课程 AI 智能对话"),
            Map.entry("CHAT_RAG", "课程 AI 智能助教 (RAG 问答)"),
            Map.entry("CHAT_TITLE", "会话标题生成"),
            Map.entry("global_assistant", "全局 AI 教学助手"),
            Map.entry("GLOBAL_ASSISTANT", "全局 AI 教学助手"),
            Map.entry("question_generate", "AI 自适应练习生成"),
            Map.entry("subjective_grading", "主观题智能批改与纠错"),
            Map.entry("summary", "章节摘要提炼"),
            Map.entry("lesson_plan", "智能教案生成"),
            Map.entry("paper_compose", "智能组卷"),
            Map.entry("GRADING", "主观题智能批改与纠错"),
            Map.entry("TEACHING_ADVICE", "AI 学情诊断与教学建议"),
            Map.entry("COURSE_OBJECTIVE", "课程教学目标 AI 推荐"),
            Map.entry("COURSE_DESCRIPTION", "课程简介 AI 生成"),
            Map.entry("COURSE_KNOWLEDGE_POINT", "课程知识点 AI 推荐"),
            Map.entry("RAG", "RAG 知识检索问答"),
            Map.entry("AGENT", "AI Agent 任务规划"),
            Map.entry("GRAPH_SUGGEST", "知识图谱关系推荐"),
            Map.entry("stream", "流式 AI 对话"),
            Map.entry("exam", "智能组卷与试题分析")
    );

    private final AiCallLogDao aiCallLogDao;
    private final SysAiQuotaDao sysAiQuotaDao;
    private final AiConverter aiConverter;
    private final SysConfigQueryApi sysConfigQueryApi;
    private final UserQueryApi userQueryApi;

    @Value("${edumind.ai.default-daily-token-limit:100000}")
    private int defaultDailyTokenLimit;

    @Override
    public PersonalAiUsageVO getMyUsage(int logDays, long pageNum, long pageSize) {
        Long userId = LoginUserResolver.resolveUserId();
        if (userId == null) {
            return emptyUsage(pageNum, pageSize);
        }

        long effectivePageNum = Math.max(pageNum, 1);
        long effectivePageSize = Math.min(Math.max(pageSize, 1), 50);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime weekStart = LocalDateTime.of(today.minusDays(6), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(today.minusDays(29), LocalTime.MIN);
        LocalDateTime semesterStart = resolveSemesterStart(today);

        List<AiCallLogEntity> todayLogs = listUserLogs(userId, todayStart, null);
        List<AiCallLogEntity> weekLogs = listUserLogs(userId, weekStart, null);
        List<AiCallLogEntity> monthLogs = listUserLogs(userId, monthStart, null);
        List<AiCallLogEntity> semesterLogs = listUserLogs(userId, semesterStart, null);
        List<AiCallLogEntity> allLogs = listUserLogs(userId, null, null);

        int todayTokens = sumTokens(todayLogs);
        int weekTokens = sumTokens(weekLogs);
        int monthTokens = sumTokens(monthLogs);
        int totalTokens = sumTokens(allLogs);

        int dailyLimit = resolveDailyTokenLimit(userId);
        int remainingPercent = dailyLimit > 0
                ? Math.max(0, Math.min(100, (int) Math.round((dailyLimit - todayTokens) * 100.0 / dailyLimit)))
                : 100;

        int todayCalls = todayLogs.size();
        int weekCalls = weekLogs.size();
        int monthCalls = monthLogs.size();
        int totalCalls = allLogs.size();

        int qaCalls = (int) allLogs.stream()
                .filter(log -> isQaOrGenerateScene(log.getScene()))
                .count();
        int avgLatency = allLogs.isEmpty() ? 0 : (int) allLogs.stream()
                .mapToInt(log -> log.getLatencyMs() != null ? log.getLatencyMs() : 0)
                .average()
                .orElse(0);

        LocalDateTime logSince = logDays > 0
                ? LocalDateTime.of(today.minusDays(logDays - 1L), LocalTime.MIN)
                : null;

        LambdaQueryWrapper<AiCallLogEntity> recentWrapper = userLogWrapper(userId)
                .orderByDesc(AiCallLogEntity::getCreateTime);
        if (logSince != null) {
            recentWrapper.ge(AiCallLogEntity::getCreateTime, logSince);
        }
        Page<AiCallLogEntity> recentPage = aiCallLogDao.page(
                new Page<>(effectivePageNum, effectivePageSize), recentWrapper);

        PersonalAiUsageVO vo = new PersonalAiUsageVO();
        vo.setTodayTokensUsed(todayTokens);
        vo.setWeekTokensUsed(weekTokens);
        vo.setMonthTokensUsed(monthTokens);
        vo.setTotalTokensUsed(totalTokens);

        vo.setTodayCalls(todayCalls);
        vo.setWeekCalls(weekCalls);
        vo.setMonthCalls(monthCalls);
        vo.setTotalCalls(totalCalls);

        vo.setTodayCostRMB(estimateCostRMB(todayTokens));
        vo.setWeekCostRMB(estimateCostRMB(weekTokens));
        vo.setMonthCostRMB(estimateCostRMB(monthTokens));
        vo.setTotalCostRMB(estimateCostRMB(totalTokens));

        vo.setDailyTokenLimit(dailyLimit);
        vo.setRemainingPercent(remainingPercent);
        vo.setQuotaStatus(resolveQuotaStatus(remainingPercent));
        vo.setTotalQaAndGenerateCalls(qaCalls);
        vo.setAvgLatencyMs(avgLatency);
        vo.setSemesterEstimatedCostRMB(estimateCostRMB(sumTokens(semesterLogs)));
        vo.setRecentLogs(recentPage.getRecords().stream().map(this::toUsageLogVO).collect(Collectors.toList()));
        vo.setTotalLogCount(recentPage.getTotal());
        vo.setLogPageNum(recentPage.getCurrent());
        vo.setLogPageSize(recentPage.getSize());
        return vo;
    }

    private PersonalAiUsageVO emptyUsage(long pageNum, long pageSize) {
        PersonalAiUsageVO vo = new PersonalAiUsageVO();
        vo.setTodayTokensUsed(0);
        vo.setWeekTokensUsed(0);
        vo.setMonthTokensUsed(0);
        vo.setTotalTokensUsed(0);
        vo.setTodayCalls(0);
        vo.setWeekCalls(0);
        vo.setMonthCalls(0);
        vo.setTotalCalls(0);
        vo.setTodayCostRMB(0D);
        vo.setWeekCostRMB(0D);
        vo.setMonthCostRMB(0D);
        vo.setTotalCostRMB(0D);
        vo.setDailyTokenLimit(resolveDailyTokenLimit(null));
        vo.setRemainingPercent(100);
        vo.setQuotaStatus("今日额度充足");
        vo.setTotalQaAndGenerateCalls(0);
        vo.setAvgLatencyMs(0);
        vo.setSemesterEstimatedCostRMB(0D);
        vo.setRecentLogs(List.of());
        vo.setTotalLogCount(0L);
        vo.setLogPageNum(Math.max(pageNum, 1));
        vo.setLogPageSize(Math.min(Math.max(pageSize, 1), 50));
        return vo;
    }

    private int resolveDailyTokenLimit(Long userId) {
        // 1. 优先检查单用户自定义独立配额（最高优先级）
        if (userId != null) {
            SysAiQuotaEntity quota = sysAiQuotaDao.findByUserId(userId);
            if (quota != null && quota.getDailyTokenLimit() != null && quota.getDailyTokenLimit() > 0) {
                return quota.getDailyTokenLimit();
            }
        }

        // 2. 检查系统全局 AI 配置中的动态配额（sys.ai.config）
        if (sysConfigQueryApi != null) {
            try {
                String aiConfigJson = sysConfigQueryApi.getConfigValueByGroup("ai");
                if (StringUtils.hasText(aiConfigJson)) {
                    JSONObject obj = JSON.parseObject(aiConfigJson);
                    if (obj != null) {
                        // 2.1 检查角色差异化配额 (多角色用户自动按最高配额生效；配额设为 0 表示该角色不限制使用 Token)
                        JSONArray roleQuotas = obj.getJSONArray("roleTokenQuotas");
                        if (roleQuotas != null && !roleQuotas.isEmpty() && userQueryApi != null && userId != null) {
                            List<String> userRoles = userQueryApi.getRolesByUserId(userId);
                            if (userRoles != null && !userRoles.isEmpty()) {
                                Integer highestQuota = null;
                                boolean hasUnlimited = false;

                                for (int i = 0; i < roleQuotas.size(); i++) {
                                    JSONObject rq = roleQuotas.getJSONObject(i);
                                    if (rq == null) continue;
                                    String roleCode = rq.getString("roleCode");
                                    Integer tokensDaily = rq.getInteger("tokensDaily");
                                    if (tokensDaily == null) {
                                        tokensDaily = rq.getInteger("maxTokensDaily");
                                    }

                                    if (roleCode != null && userRoles.contains(roleCode) && tokensDaily != null) {
                                        if (tokensDaily == 0) {
                                            hasUnlimited = true;
                                        } else if (highestQuota == null || tokensDaily > highestQuota) {
                                            highestQuota = tokensDaily;
                                        }
                                    }
                                }

                                if (hasUnlimited) {
                                    return 10_000_000;
                                }
                                if (highestQuota != null && highestQuota > 0) {
                                    return highestQuota;
                                }
                            }
                        }

                        // 2.2 全局每用户默认每日配额
                        Integer tokensPerUserDaily = obj.getInteger("tokensPerUserDaily");
                        if (tokensPerUserDaily != null && tokensPerUserDaily > 0) {
                            return tokensPerUserDaily;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 3. 兜底回退为 Spring 配置中的默认值
        return defaultDailyTokenLimit;
    }

    private List<AiCallLogEntity> listUserLogs(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = userLogWrapper(userId);
        if (start != null) {
            wrapper.ge(AiCallLogEntity::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(AiCallLogEntity::getCreateTime, end);
        }
        return aiCallLogDao.list(wrapper);
    }

    private LambdaQueryWrapper<AiCallLogEntity> userLogWrapper(Long userId) {
        return new LambdaQueryWrapper<AiCallLogEntity>().eq(AiCallLogEntity::getUserId, userId);
    }

    private int sumTokens(List<AiCallLogEntity> logs) {
        return logs.stream().mapToInt(this::totalTokensOf).sum();
    }

    private int totalTokensOf(AiCallLogEntity log) {
        int prompt = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
        int completion = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;
        return prompt + completion;
    }

    private double estimateCostRMB(int tokens) {
        return (tokens / 1000.0) * 0.002;
    }

    private boolean isQaOrGenerateScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return false;
        }
        return QA_AND_GENERATE_SCENES.contains(scene) || QA_AND_GENERATE_SCENES.contains(scene.toUpperCase());
    }

    private String resolveQuotaStatus(int remainingPercent) {
        if (remainingPercent >= 50) {
            return "今日额度充足";
        }
        if (remainingPercent >= 20) {
            return "今日额度紧张";
        }
        if (remainingPercent > 0) {
            return "今日额度即将用尽";
        }
        return "今日额度已用尽";
    }

    private LocalDateTime resolveSemesterStart(LocalDate today) {
        int year = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return LocalDateTime.of(LocalDate.of(year, 9, 1), LocalTime.MIN);
    }

    private PersonalAiUsageLogVO toUsageLogVO(AiCallLogEntity entity) {
        AiCallLogVO callLog = aiConverter.toCallLogVO(entity);
        PersonalAiUsageLogVO vo = new PersonalAiUsageLogVO();
        vo.setId(callLog.getId());
        vo.setScene(callLog.getScene());
        vo.setSceneLabel(resolveSceneLabel(callLog.getScene()));
        vo.setModel(normalizeModelDisplay(callLog.getModel()));
        vo.setTotalTokens(callLog.getTotalTokens());
        vo.setCreateTime(callLog.getCreateTime());
        return vo;
    }

    private String resolveSceneLabel(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "AI 功能调用";
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

    /** 兼容历史记录中的「配置名 · 模型名」格式，仅展示模型名 */
    private String normalizeModelDisplay(String model) {
        if (!StringUtils.hasText(model)) {
            return "unknown";
        }
        int separator = model.indexOf(" · ");
        return separator >= 0 ? model.substring(separator + 3).trim() : model;
    }
}
