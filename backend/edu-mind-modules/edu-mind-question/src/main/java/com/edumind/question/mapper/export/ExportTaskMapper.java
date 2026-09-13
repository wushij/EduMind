package com.edumind.question.mapper.export;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.question.entity.export.ExportTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导出任务 Mapper
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTaskEntity> {
}
