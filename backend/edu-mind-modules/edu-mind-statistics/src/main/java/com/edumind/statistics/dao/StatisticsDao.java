package com.edumind.statistics.dao;

import com.edumind.statistics.entity.StatisticsEntity;
import com.edumind.statistics.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 统计分析数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class StatisticsDao {

    private final StatisticsMapper statisticsMapper;

    public StatisticsEntity findById(Long id) {
        return statisticsMapper.selectById(id);
    }

    public List<StatisticsEntity> findAll() {
        return statisticsMapper.selectList(null);
    }
}
