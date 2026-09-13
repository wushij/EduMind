package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysSmsLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信发送日志 Mapper
 */
@Mapper
public interface SysSmsLogMapper extends BaseMapper<SysSmsLogEntity> {
}
