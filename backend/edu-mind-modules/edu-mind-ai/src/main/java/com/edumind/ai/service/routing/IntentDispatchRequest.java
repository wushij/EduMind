package com.edumind.ai.service.routing;

import com.edumind.ai.router.IntentRouter;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntentDispatchRequest {
    private String message;
    private Long courseId;
    private Long knowledgeBaseId;
    private Long documentId;
    private IntentRouter.IntentResult intent;
    private String courseName;
    private String conversationHistory;
    /** 向量检索用短问句；为空时回退 {@link #message} */
    private String retrievalQuery;
    private String contextModule;
    private Long lessonChapterId;
    /** 课节虚拟文档 ID，检索时优先 scoped */
    private Long lessonDocumentId;
    /** 课节场景 enrichment，追加到 RAG system prompt */
    private String lessonEnrichmentBlock;
}
