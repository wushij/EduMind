package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentDao {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeDocumentEntity findById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    public List<KnowledgeDocumentEntity> findByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return Collections.emptyList();
        }
        return knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeDocumentEntity::getCreateTime)
        );
    }

    public int insert(KnowledgeDocumentEntity entity) {
        return knowledgeDocumentMapper.insert(entity);
    }

    public int updateById(KnowledgeDocumentEntity entity) {
        return knowledgeDocumentMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return knowledgeDocumentMapper.deleteById(id);
    }

    public long countByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeDocumentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
        );
    }
}
