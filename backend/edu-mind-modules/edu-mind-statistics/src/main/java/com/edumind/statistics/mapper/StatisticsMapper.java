package com.edumind.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.statistics.entity.StatisticsEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统计分析持久层 Mapper
 */
@Mapper
public interface StatisticsMapper extends BaseMapper<StatisticsEntity> {
}
