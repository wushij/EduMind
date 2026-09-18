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

    public List<Long> findIdsByPhraseMatch(Long knowledgeBaseId, Long documentId, String phrase, int limit) {
        if (knowledgeBaseId == null || !StringUtils.hasText(phrase) || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KnowledgeDocumentChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                .select(KnowledgeDocumentChunkEntity::getId)
                .eq(KnowledgeDocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .like(KnowledgeDocumentChunkEntity::getContent, phrase.trim())
                .orderByAsc(KnowledgeDocumentChunkEntity::getDocumentId)
                .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex)
                .last("LIMIT " + limit);
        if (documentId != null) {
            wrapper.eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId);
        }
        return knowledgeDocumentChunkMapper.selectList(wrapper).stream()
                .map(KnowledgeDocumentChunkEntity::getId)
                .toList();
    }

    public List<Long> findIdsByTokenOrMatch(Long knowledgeBaseId, Long documentId, List<String> tokens, int limit) {
        if (knowledgeBaseId == null || tokens == null || tokens.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KnowledgeDocumentChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                .select(KnowledgeDocumentChunkEntity::getId)
                .eq(KnowledgeDocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .and(w -> {
                    boolean first = true;
                    for (String token : tokens) {
                        if (!StringUtils.hasText(token) || token.length() < 2) {
                            continue;
                        }
                        if (first) {
                            w.like(KnowledgeDocumentChunkEntity::getContent, token.trim());
                            first = false;
                        } else {
                            w.or().like(KnowledgeDocumentChunkEntity::getContent, token.trim());
                        }
                    }
                })
                .orderByAsc(KnowledgeDocumentChunkEntity::getDocumentId)
                .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex)
                .last("LIMIT " + limit);
        if (documentId != null) {
            wrapper.eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId);
        }
        return knowledgeDocumentChunkMapper.selectList(wrapper).stream()
                .map(KnowledgeDocumentChunkEntity::getId)
                .toList();
    }

    public List<Long> findIdsByTechTermsAndMatch(Long knowledgeBaseId, Long documentId, List<String> englishTokens,
                                                 int limit) {
        if (knowledgeBaseId == null || englishTokens == null || englishTokens.size() < 2 || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KnowledgeDocumentChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentChunkEntity>()
                .select(KnowledgeDocumentChunkEntity::getId)
                .eq(KnowledgeDocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId);
        if (documentId != null) {
            wrapper.eq(KnowledgeDocumentChunkEntity::getDocumentId, documentId);
        }
        for (String token : englishTokens) {
            wrapper.like(KnowledgeDocumentChunkEntity::getContent, token);
        }
        wrapper.orderByAsc(KnowledgeDocumentChunkEntity::getDocumentId)
                .orderByAsc(KnowledgeDocumentChunkEntity::getChunkIndex)
                .last("LIMIT " + limit);
        return knowledgeDocumentChunkMapper.selectList(wrapper).stream()
                .map(KnowledgeDocumentChunkEntity::getId)
                .toList();
    }
}
