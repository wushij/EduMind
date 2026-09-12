package com.edumind.ai.controller.audit;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.service.audit.TokenStatisticsService;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.audit.TokenAuditSummaryVO;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/ai-audit")
@RequiredArgsConstructor
public class TokenStatisticsController {

    private final TokenStatisticsService tokenStatisticsService;

    @SaCheckPermission("system:audit:view")
    @GetMapping("/logs")
    public ApiResult<PageResult<AiCallLogVO>> logs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResult.success(tokenStatisticsService.listLogs(
                userId, scene, model, startDate, endDate, page, pageSize));
    }

    @SaCheckPermission("system:audit:view")
    @GetMapping("/summary")
    public ApiResult<TokenAuditSummaryVO> summary(
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(tokenStatisticsService.summary(groupBy, startDate, endDate));
    }

    @SaCheckPermission("system:audit:view")
    @GetMapping("/daily-trend")
    public ApiResult<List<Map<String, Object>>> dailyTrend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(tokenStatisticsService.dailyTrend(days, endDate));
    }
}
