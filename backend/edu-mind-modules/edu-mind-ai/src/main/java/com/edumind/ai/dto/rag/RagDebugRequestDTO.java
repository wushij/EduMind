package com.edumind.ai.dto.rag;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RagDebugRequestDTO extends RetrievalRequestDTO {
    private Long knowledgeBaseId;
    private Boolean debugMode = true;
    private Boolean skipLlm = false;
}
