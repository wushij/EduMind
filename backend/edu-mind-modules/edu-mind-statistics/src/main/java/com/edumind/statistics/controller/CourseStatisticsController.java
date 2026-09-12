package com.edumind.statistics.controller;

import com.edumind.common.api.ApiResult;
import com.edumind.statistics.dao.CourseStatisticsDao;
import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.job.CourseStatisticsJob;
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

    private final CourseStatisticsJob courseStatisticsJob;
    private final CourseStatisticsDao courseStatisticsDao;

    @PostMapping("/aggregate")
    public ApiResult<CourseStatisticsEntity> triggerAggregate(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        CourseStatisticsEntity entity = courseStatisticsJob.aggregateCourseStat(courseId, date);
        return ApiResult.success(entity);
    }

    @GetMapping("/daily")
    public ApiResult<List<CourseStatisticsEntity>> getDailyStats(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        List<CourseStatisticsEntity> list = courseStatisticsDao.listByCourseAndDateRange(courseId, startDate, endDate);
        return ApiResult.success(list);
    }
}
