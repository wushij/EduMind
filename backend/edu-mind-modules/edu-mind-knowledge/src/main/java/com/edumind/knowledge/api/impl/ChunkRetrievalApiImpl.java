package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.ChunkRetrievalApi;
import com.edumind.knowledge.service.query.ChunkRetrievalService;
import com.edumind.knowledge.vo.knowledge.ChunkKeywordSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChunkRetrievalApiImpl implements ChunkRetrievalApi {

    private final ChunkRetrievalService chunkRetrievalService;

    @Override
    public ChunkKeywordSearchVO searchKeywords(Long knowledgeBaseId, Long documentId, String query, int limit) {
        return chunkRetrievalService.searchKeywords(knowledgeBaseId, documentId, query, limit);
    }
}
