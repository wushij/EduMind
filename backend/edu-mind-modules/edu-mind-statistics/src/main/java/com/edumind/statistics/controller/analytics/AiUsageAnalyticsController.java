package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.analytics.AiUsageAnalyticsService;
import com.edumind.statistics.vo.analytics.AiUsageAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/ai-usage")
@RequiredArgsConstructor
public class AiUsageAnalyticsController {

    private final AiUsageAnalyticsService aiUsageAnalyticsService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<AiUsageAnalyticsVO> getAiUsage(
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "7d") String range) {
        return ApiResult.success(aiUsageAnalyticsService.getUsage(courseId, range));
    }
}
