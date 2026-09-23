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
    /**
     * 悬浮 AI 助手「全域通用教学空间」：未锚定任何课程 / 课节 / 题库。
     *
     * <p>该场景没有课程上下文可依，必须按平台能力口径作答，因此走 global_assistant 模板
     * 而不是课程助教的 chat 模板——否则模型会凭通用知识编造平台并不具备的功能。</p>
     */
    private boolean globalScope;
    private Long lessonChapterId;
    /** 课节虚拟文档 ID，检索时优先 scoped */
    private Long lessonDocumentId;
    /** 课节场景 enrichment，追加到 RAG system prompt */
    private String lessonEnrichmentBlock;
}
