package com.edumind.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.course.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程 Mapper
 */
@Mapper
public interface CourseMapper extends BaseMapper<CourseEntity> {
}
