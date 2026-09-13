package com.edumind.statistics.converter;

import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.vo.analytics.CourseStatisticsVO;

import java.util.List;
import java.util.stream.Collectors;

public final class CourseStatisticsConverter {

    private CourseStatisticsConverter() {
    }

    public static CourseStatisticsVO toVO(CourseStatisticsEntity entity) {
        if (entity == null) {
            return null;
        }
        CourseStatisticsVO vo = new CourseStatisticsVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setStatDate(entity.getStatDate());
        vo.setStudentCount(entity.getStudentCount());
        vo.setAvgScore(entity.getAvgScore());
        vo.setMasteryAvg(entity.getMasteryAvg());
        vo.setAiCallCount(entity.getAiCallCount());
        vo.setWrongCount(entity.getWrongCount());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<CourseStatisticsVO> toVOList(List<CourseStatisticsEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(CourseStatisticsConverter::toVO).collect(Collectors.toList());
    }
}
