package com.edumind.ai.service.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.service.audit.TokenStatisticsService;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.audit.TokenAuditSummaryVO;
import com.edumind.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenStatisticsServiceImpl implements TokenStatisticsService {

    private final AiCallLogDao aiCallLogDao;
    private final AiConverter aiConverter;

    @Override
    public PageResult<AiCallLogVO> listLogs(Long userId, String scene, String model,
                                            LocalDate startDate, LocalDate endDate,
                                            long page, long pageSize) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = buildLogWrapper(userId, scene, model, startDate, endDate);
        wrapper.orderByDesc(AiCallLogEntity::getCreateTime);
        Page<AiCallLogEntity> result = aiCallLogDao.page(new Page<>(page, pageSize), wrapper);
        List<AiCallLogVO> list = result.getRecords().stream()
                .map(aiConverter::toCallLogVO)
                .collect(Collectors.toList());
        return PageResult.<AiCallLogVO>builder()
                .total(result.getTotal())
                .pageNum(page)
                .pageSize(pageSize)
                .list(list)
                .build();
    }

    @Override
    public TokenAuditSummaryVO summary(String groupBy, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(6);
        List<AiCallLogEntity> logs = aiCallLogDao.list(buildLogWrapper(null, null, null, effectiveStart, effectiveEnd));
        int promptTokens = logs.stream().mapToInt(log -> log.getPromptTokens() != null ? log.getPromptTokens() : 0).sum();
        int completionTokens = logs.stream().mapToInt(log -> log.getCompletionTokens() != null ? log.getCompletionTokens() : 0).sum();
        int avgLatency = logs.isEmpty() ? 0 : (int) logs.stream()
                .mapToInt(log -> log.getLatencyMs() != null ? log.getLatencyMs() : 0).average().orElse(0);
        TokenAuditSummaryVO vo = new TokenAuditSummaryVO();
        vo.setTotalPromptTokens(promptTokens);
        vo.setTotalCompletionTokens(completionTokens);
        vo.setTotalCalls(logs.size());
        vo.setAvgLatencyMs(avgLatency);
        vo.setGroupBy(groupBy);
        vo.setDailyTrend(buildDailyTrend(logs, effectiveStart, effectiveEnd));
        vo.setModelDistribution(buildModelDistribution(logs));
        return vo;
    }

    @Override
    public List<Map<String, Object>> dailyTrend(int days, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = effectiveEnd.minusDays(Math.max(days, 1) - 1);
        List<AiCallLogEntity> logs = aiCallLogDao.list(buildLogWrapper(null, null, null, effectiveStart, effectiveEnd));
        return buildDailyTrend(logs, effectiveStart, effectiveEnd);
    }

    private LambdaQueryWrapper<AiCallLogEntity> buildLogWrapper(Long userId, String scene, String model,
                                                                LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AiCallLogEntity::getUserId, userId);
        }
        if (scene != null) {
            wrapper.eq(AiCallLogEntity::getScene, scene);
        }
        if (model != null) {
            wrapper.eq(AiCallLogEntity::getModel, model);
        }
        if (startDate != null) {
            wrapper.ge(AiCallLogEntity::getCreateTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(AiCallLogEntity::getCreateTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        return wrapper;
    }

    private List<Map<String, Object>> buildDailyTrend(List<AiCallLogEntity> logs, LocalDate start, LocalDate end) {
        Map<LocalDate, Integer> tokenByDay = logs.stream().collect(Collectors.groupingBy(
                log -> log.getCreateTime().toLocalDate(),
                Collectors.summingInt(log -> (log.getPromptTokens() != null ? log.getPromptTokens() : 0)
                        + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0))
        ));
        Map<LocalDate, Long> callsByDay = logs.stream().collect(Collectors.groupingBy(
                log -> log.getCreateTime().toLocalDate(),
                Collectors.counting()
        ));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("tokens", tokenByDay.getOrDefault(date, 0));
            item.put("calls", callsByDay.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildModelDistribution(List<AiCallLogEntity> logs) {
        Map<String, Integer> tokensByModel = new LinkedHashMap<>();
        for (AiCallLogEntity log : logs) {
            String model = log.getModel() != null ? log.getModel() : "unknown";
            int tokens = (log.getPromptTokens() != null ? log.getPromptTokens() : 0)
                    + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0);
            tokensByModel.merge(model, tokens, Integer::sum);
        }
        return tokensByModel.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("model", entry.getKey());
                    item.put("tokens", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }
}
