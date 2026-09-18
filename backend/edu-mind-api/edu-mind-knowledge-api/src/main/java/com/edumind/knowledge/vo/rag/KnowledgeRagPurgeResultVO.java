package com.edumind.knowledge.vo.rag;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeRagPurgeResultVO {

    private int purgedChunks;
    private int purgedIndexRows;
}
