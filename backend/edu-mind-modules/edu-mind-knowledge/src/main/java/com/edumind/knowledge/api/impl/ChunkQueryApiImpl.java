package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.ChunkQueryApi;
import com.edumind.knowledge.converter.ChunkConverter;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkQueryApiImpl implements ChunkQueryApi {

    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final ChunkConverter chunkConverter;

    @Override
    public List<ChunkVO> listByDocumentId(Long documentId, long page, long pageSize) {
        if (documentId == null) {
            return Collections.emptyList();
        }
        return chunkConverter.toVOList(
                knowledgeDocumentChunkDao.pageByDocumentId(documentId, page, pageSize, null).getRecords()
        );
    }

    @Override
    public List<ChunkVO> listByIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeDocumentChunkEntity> entities = knowledgeDocumentChunkDao.findByIds(chunkIds);
        return chunkConverter.toVOList(entities);
    }

    @Override
    public int countByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return 0;
        }
        return (int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId);
    }
}
