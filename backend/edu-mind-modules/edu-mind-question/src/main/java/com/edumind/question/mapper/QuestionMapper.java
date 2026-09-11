package com.edumind.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.question.entity.QuestionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<QuestionEntity> {
}
