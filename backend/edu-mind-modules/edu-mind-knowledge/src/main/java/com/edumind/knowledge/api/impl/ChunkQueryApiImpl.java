package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.ChunkQueryApi;
import com.edumind.knowledge.service.query.ChunkQueryService;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkQueryApiImpl implements ChunkQueryApi {

    private final ChunkQueryService chunkQueryService;

    @Override
    public List<ChunkVO> listByDocumentId(Long documentId, long page, long pageSize) {
        return chunkQueryService.listByDocumentId(documentId, page, pageSize);
    }

    @Override
    public List<ChunkVO> listByIds(List<Long> chunkIds) {
        return chunkQueryService.listByIds(chunkIds);
    }

    @Override
    public int countByKnowledgeBaseId(Long knowledgeBaseId) {
        return chunkQueryService.countByKnowledgeBaseId(knowledgeBaseId);
    }
}
