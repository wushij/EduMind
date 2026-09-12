package com.edumind.knowledge.api;

import com.edumind.knowledge.vo.knowledge.ChunkVO;

import java.util.List;

public interface ChunkQueryApi {

    List<ChunkVO> listByDocumentId(Long documentId, long page, long pageSize);

    List<ChunkVO> listByIds(List<Long> chunkIds);

    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
