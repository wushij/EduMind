package com.edumind.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.teaching.entity.ExamEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试 Mapper
 */
@Mapper
public interface ExamMapper extends BaseMapper<ExamEntity> {
}
