package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.mapper.KnowledgeDocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentChunkDao {

    private final KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper;

    public KnowledgeDocumentChunkEntity findById(Long id) {
        return knowledgeDocumentChunkMapper.selectById(id);
    }

    public List<KnowledgeDocumentChunkEntity> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeDocumentChunkMapper.selectBatchIds(ids);
    }

    public Page<KnowledgeDocumentChunkEntity> pageByDocumentId(Long documentId, long page, long pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeDocumentChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                .eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId)
                .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KnowledgeDocumentChunkEntity::getContent, keyword);
        }
        return knowledgeDocumentChunkMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public List<KnowledgeDocumentChunkEntity> findByDocumentId(Long documentId) {
        if (documentId == null) {
            return Collections.emptyList();
        }
        return knowledgeDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                        .eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId)
                        .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex)
        );
    }

    public List<KnowledgeDocumentChunkEntity> findByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return Collections.emptyList();
        }
        return knowledgeDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                        .eq(KnowledgeDocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(KnowledgeDocumentChunkEntity::getDocumentId)
                        .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex)
        );
    }

    public long countByDocumentId(Long documentId) {
        return knowledgeDocumentChunkMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                        .eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId)
        );
    }

    public long countByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeDocumentChunkMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                        .eq(KnowledgeDocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    public int insert(KnowledgeDocumentChunkEntity entity) {
        return knowledgeDocumentChunkMapper.insert(entity);
    }

    public int deleteByDocumentId(Long documentId) {
        return knowledgeDocumentChunkMapper.delete(
                new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                        .eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId)
        );
    }
}
