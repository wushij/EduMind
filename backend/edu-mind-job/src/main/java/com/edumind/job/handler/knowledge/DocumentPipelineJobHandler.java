package com.edumind.job.handler.knowledge;

import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPipelineJobHandler {

    private final DocumentPipelineService documentPipelineService;

    public void handleParseAndChunk(Long documentId) {
        log.info("Job: parse and chunk documentId={}", documentId);
        documentPipelineService.parseAndChunkAsync(documentId);
    }
}
