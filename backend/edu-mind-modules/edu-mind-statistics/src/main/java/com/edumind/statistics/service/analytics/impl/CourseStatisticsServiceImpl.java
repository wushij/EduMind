package com.edumind.statistics.service.analytics.impl;

import com.edumind.statistics.converter.CourseStatisticsConverter;
import com.edumind.statistics.dao.CourseStatisticsDao;
import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.job.CourseStatisticsJob;
import com.edumind.statistics.service.analytics.CourseStatisticsService;
import com.edumind.statistics.vo.analytics.CourseStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseStatisticsServiceImpl implements CourseStatisticsService {

    private final CourseStatisticsJob courseStatisticsJob;
    private final CourseStatisticsDao courseStatisticsDao;

    @Override
    public CourseStatisticsVO triggerAggregate(Long courseId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);
        CourseStatisticsEntity entity = courseStatisticsJob.aggregateCourseStat(courseId, targetDate);
        return CourseStatisticsConverter.toVO(entity);
    }

    @Override
    public List<CourseStatisticsVO> listDailyStats(Long courseId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return CourseStatisticsConverter.toVOList(
                courseStatisticsDao.listByCourseAndDateRange(courseId, start, end));
    }
}
