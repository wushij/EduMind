package com.edumind.system.mapper.security;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 国密密钥版本 MyBatis Mapper
 */
@Mapper
public interface SecurityKeyVersionMapper extends BaseMapper<SecurityKeyVersionEntity> {
}
