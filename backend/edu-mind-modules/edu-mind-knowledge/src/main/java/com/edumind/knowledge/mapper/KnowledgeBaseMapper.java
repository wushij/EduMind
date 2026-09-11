package com.edumind.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库持久层 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
}
