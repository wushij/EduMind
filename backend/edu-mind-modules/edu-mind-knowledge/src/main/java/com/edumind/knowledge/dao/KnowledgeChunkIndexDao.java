package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgeChunkIndexEntity;
import com.edumind.knowledge.mapper.KnowledgeChunkIndexMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeChunkIndexDao {

    private final KnowledgeChunkIndexMapper knowledgeChunkIndexMapper;

    public KnowledgeChunkIndexEntity findByChunkId(Long chunkId) {
        return knowledgeChunkIndexMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getChunkId, chunkId)
        );
    }

    public List<KnowledgeChunkIndexEntity> findFailedByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeChunkIndexMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeChunkIndexEntity::getEmbedStatus, "FAILED")
        );
    }

    public long countIndexedByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeChunkIndexMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeChunkIndexEntity::getEmbedStatus, "INDEXED")
        );
    }

    public long countFailedByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeChunkIndexMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeChunkIndexEntity::getEmbedStatus, "FAILED")
        );
    }

    public int insert(KnowledgeChunkIndexEntity entity) {
        return knowledgeChunkIndexMapper.insert(entity);
    }

    public int updateById(KnowledgeChunkIndexEntity entity) {
        return knowledgeChunkIndexMapper.updateById(entity);
    }

    public int deleteByDocumentId(Long documentId) {
        return knowledgeChunkIndexMapper.delete(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getDocumentId, documentId)
        );
    }

    public List<KnowledgeChunkIndexEntity> listIndexedWithEmbeddingVectors(long offset, int batchSize) {
        return knowledgeChunkIndexMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChunkIndexEntity>()
                        .eq(KnowledgeChunkIndexEntity::getEmbedStatus, "INDEXED")
                        .isNotNull(KnowledgeChunkIndexEntity::getEmbeddingVector)
                        .orderByAsc(KnowledgeChunkIndexEntity::getId)
                        .last("LIMIT " + batchSize + " OFFSET " + offset)
        );
    }
}
