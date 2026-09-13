package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysEmailLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送日志 Mapper
 */
@Mapper
public interface SysEmailLogMapper extends BaseMapper<SysEmailLogEntity> {
}
