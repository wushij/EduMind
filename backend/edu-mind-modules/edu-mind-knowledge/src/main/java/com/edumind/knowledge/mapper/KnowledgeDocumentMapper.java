package com.edumind.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {
}
