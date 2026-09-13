package com.edumind.statistics.service.analytics;

import com.edumind.statistics.vo.analytics.CourseStatisticsVO;

import java.time.LocalDate;
import java.util.List;

public interface CourseStatisticsService {

    CourseStatisticsVO triggerAggregate(Long courseId, LocalDate date);

    List<CourseStatisticsVO> listDailyStats(Long courseId, LocalDate startDate, LocalDate endDate);
}
