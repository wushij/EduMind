package com.edumind.knowledge.service.chunk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChunkTriggerExecutor {

    private final ChunkService chunkService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerChunkInNewTransaction(Long documentId) {
        chunkService.triggerChunk(documentId);
    }
}
