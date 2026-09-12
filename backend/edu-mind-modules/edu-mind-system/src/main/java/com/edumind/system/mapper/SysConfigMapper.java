package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统全局参数配置持久层 Mapper
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {
}
