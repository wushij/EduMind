package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.mapper.KnowledgeDocumentTextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentTextDao {

    private final KnowledgeDocumentTextMapper knowledgeDocumentTextMapper;

    public KnowledgeDocumentTextEntity findByDocumentId(Long documentId) {
        return knowledgeDocumentTextMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocumentTextEntity>()
                        .eq(KnowledgeDocumentTextEntity::getDocumentId, documentId)
                        .last("LIMIT 1")
        );
    }

    public int insert(KnowledgeDocumentTextEntity entity) {
        return knowledgeDocumentTextMapper.insert(entity);
    }

    public int updateById(KnowledgeDocumentTextEntity entity) {
        return knowledgeDocumentTextMapper.updateById(entity);
    }

    public int deleteByDocumentId(Long documentId) {
        return knowledgeDocumentTextMapper.delete(
                new LambdaQueryWrapper<KnowledgeDocumentTextEntity>()
                        .eq(KnowledgeDocumentTextEntity::getDocumentId, documentId)
        );
    }
}
