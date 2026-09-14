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
}
