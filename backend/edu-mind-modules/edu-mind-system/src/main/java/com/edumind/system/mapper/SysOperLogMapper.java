package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysOperLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志底层持久化 Mapper
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLogEntity> {
}
