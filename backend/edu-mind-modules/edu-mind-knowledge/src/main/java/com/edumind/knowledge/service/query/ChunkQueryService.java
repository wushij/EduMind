package com.edumind.knowledge.service.query;

import com.edumind.knowledge.vo.knowledge.ChunkVO;

import java.util.List;

public interface ChunkQueryService {

    List<ChunkVO> listByDocumentId(Long documentId, long page, long pageSize);

    List<ChunkVO> listByIds(List<Long> chunkIds);

    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
