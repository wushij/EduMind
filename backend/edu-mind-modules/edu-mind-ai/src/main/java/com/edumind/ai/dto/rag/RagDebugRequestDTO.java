package com.edumind.ai.dto.rag;

import lombok.Data;

@Data
public class RagDebugRequestDTO extends RetrievalRequestDTO {
    private Long knowledgeBaseId;
    private Boolean debugMode = true;
    private Boolean skipLlm = false;
}
