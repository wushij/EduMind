package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户持久层 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
