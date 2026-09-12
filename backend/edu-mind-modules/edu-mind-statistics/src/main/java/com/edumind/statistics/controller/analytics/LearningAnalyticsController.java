package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/learning")
@RequiredArgsConstructor
public class LearningAnalyticsController {

    private final LearningAnalyticsService learningAnalyticsService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<LearningAnalyticsVO> getLearningAnalytics(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) Long classId) {
        return ApiResult.success(learningAnalyticsService.getLearningAnalytics(courseId, range, classId));
    }
}
