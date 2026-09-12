package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import com.edumind.knowledge.service.knowledge.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPipelineServiceImpl implements DocumentPipelineService {

    private final DocumentService documentService;
    private final ChunkService chunkService;
    private final IndexingService indexingService;

    @Override
    @Async("knowledgeTaskExecutor")
    public void parseAndChunkAsync(Long documentId) {
        try {
            documentService.triggerParse(documentId);
            chunkService.triggerChunk(documentId);
            indexingService.reindexDocument(documentId);
        } catch (Exception ex) {
            log.warn("文档流水线失败 documentId={}: {}", documentId, ex.getMessage());
        }
    }
}
