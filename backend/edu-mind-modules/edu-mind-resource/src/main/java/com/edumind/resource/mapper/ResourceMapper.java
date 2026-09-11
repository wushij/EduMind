package com.edumind.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.resource.entity.ResourceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学资源 Mapper
 */
@Mapper
public interface ResourceMapper extends BaseMapper<ResourceEntity> {
}
