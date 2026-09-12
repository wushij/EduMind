package com.edumind.knowledge.service.knowledge;

public interface DocumentPipelineService {

    void parseAndChunkAsync(Long documentId);
}
