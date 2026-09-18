package com.edumind.knowledge.vo.rag;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeRagSyncResultVO {

    private String status;
    private int knowledgeBasesTriggered;
    private int lessonsReindexed;
    private Long documentId;
}
