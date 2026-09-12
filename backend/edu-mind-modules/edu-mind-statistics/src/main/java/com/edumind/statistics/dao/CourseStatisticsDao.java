package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.mapper.CourseStatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseStatisticsDao {

    private final CourseStatisticsMapper courseStatisticsMapper;

    public CourseStatisticsEntity findByCourseAndDate(Long courseId, LocalDate date) {
        return courseStatisticsMapper.selectOne(new LambdaQueryWrapper<CourseStatisticsEntity>()
                .eq(CourseStatisticsEntity::getCourseId, courseId)
                .eq(CourseStatisticsEntity::getStatDate, date)
                .last("LIMIT 1"));
    }

    public CourseStatisticsEntity findLatestByCourse(Long courseId) {
        return courseStatisticsMapper.selectOne(new LambdaQueryWrapper<CourseStatisticsEntity>()
                .eq(CourseStatisticsEntity::getCourseId, courseId)
                .orderByDesc(CourseStatisticsEntity::getStatDate)
                .last("LIMIT 1"));
    }

    public List<CourseStatisticsEntity> listByCourseAndDateRange(Long courseId, LocalDate startDate, LocalDate endDate) {
        return courseStatisticsMapper.selectList(new LambdaQueryWrapper<CourseStatisticsEntity>()
                .eq(CourseStatisticsEntity::getCourseId, courseId)
                .ge(startDate != null, CourseStatisticsEntity::getStatDate, startDate)
                .le(endDate != null, CourseStatisticsEntity::getStatDate, endDate)
                .orderByAsc(CourseStatisticsEntity::getStatDate));
    }

    public void saveOrUpdate(CourseStatisticsEntity entity) {
        CourseStatisticsEntity existing = findByCourseAndDate(entity.getCourseId(), entity.getStatDate());
        if (existing == null) {
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            courseStatisticsMapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            entity.setUpdateTime(LocalDateTime.now());
            courseStatisticsMapper.updateById(entity);
        }
    }
}
