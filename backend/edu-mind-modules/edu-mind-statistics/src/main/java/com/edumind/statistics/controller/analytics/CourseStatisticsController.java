package com.edumind.statistics.controller.analytics;

import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.analytics.CourseStatisticsService;
import com.edumind.statistics.vo.analytics.CourseStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics/course-statistics")
@RequiredArgsConstructor
public class CourseStatisticsController {

    private final CourseStatisticsService courseStatisticsService;

    @PostMapping("/aggregate")
    public ApiResult<CourseStatisticsVO> triggerAggregate(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResult.success(courseStatisticsService.triggerAggregate(courseId, date));
    }

    @GetMapping("/daily")
    public ApiResult<List<CourseStatisticsVO>> getDailyStats(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(courseStatisticsService.listDailyStats(courseId, startDate, endDate));
    }
}
