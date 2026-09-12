package com.edumind.ai.controller.audit;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/ai-audit")
@RequiredArgsConstructor
public class TokenStatisticsController {

    private final AiCallLogDao aiCallLogDao;

    @SaCheckPermission("system:audit:view")
    @GetMapping("/logs")
    public ApiResult<PageResult<AiCallLogEntity>> logs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
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
        wrapper.orderByDesc(AiCallLogEntity::getCreateTime);
        Page<AiCallLogEntity> result = aiCallLogDao.page(new Page<>(page, pageSize), wrapper);
        return ApiResult.success(PageResult.<AiCallLogEntity>builder()
                .total(result.getTotal())
                .pageNum(page)
                .pageSize(pageSize)
                .list(result.getRecords())
                .build());
    }

    @SaCheckPermission("system:audit:view")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary(
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(6);
        List<AiCallLogEntity> logs = aiCallLogDao.list(new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(AiCallLogEntity::getCreateTime, LocalDateTime.of(effectiveStart, LocalTime.MIN))
                .le(AiCallLogEntity::getCreateTime, LocalDateTime.of(effectiveEnd, LocalTime.MAX)));
        int promptTokens = logs.stream().mapToInt(log -> log.getPromptTokens() != null ? log.getPromptTokens() : 0).sum();
        int completionTokens = logs.stream().mapToInt(log -> log.getCompletionTokens() != null ? log.getCompletionTokens() : 0).sum();
        int avgLatency = logs.isEmpty() ? 0 : (int) logs.stream()
                .mapToInt(log -> log.getLatencyMs() != null ? log.getLatencyMs() : 0).average().orElse(0);
        Map<String, Object> data = new HashMap<>();
        data.put("totalPromptTokens", promptTokens);
        data.put("totalCompletionTokens", completionTokens);
        data.put("totalCalls", logs.size());
        data.put("avgLatencyMs", avgLatency);
        data.put("groupBy", groupBy);
        data.put("dailyTrend", buildDailyTrend(logs, effectiveStart, effectiveEnd));
        data.put("modelDistribution", buildModelDistribution(logs));
        return ApiResult.success(data);
    }

    @SaCheckPermission("system:audit:view")
    @GetMapping("/daily-trend")
    public ApiResult<List<Map<String, Object>>> dailyTrend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = effectiveEnd.minusDays(Math.max(days, 1) - 1);
        List<AiCallLogEntity> logs = aiCallLogDao.list(new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(AiCallLogEntity::getCreateTime, LocalDateTime.of(effectiveStart, LocalTime.MIN))
                .le(AiCallLogEntity::getCreateTime, LocalDateTime.of(effectiveEnd, LocalTime.MAX)));
        return ApiResult.success(buildDailyTrend(logs, effectiveStart, effectiveEnd));
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
