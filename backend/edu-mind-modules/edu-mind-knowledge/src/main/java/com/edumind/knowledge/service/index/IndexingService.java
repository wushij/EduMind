package com.edumind.knowledge.service.index;

import com.edumind.knowledge.vo.knowledge.IndexStatusVO;

public interface IndexingService {

    void triggerIndex(Long knowledgeBaseId, String mode);

    IndexStatusVO getIndexStatus(Long knowledgeBaseId);

    void reindexDocument(Long documentId);

    void reindexChunk(Long knowledgeBaseId, Long chunkId);
}
