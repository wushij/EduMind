package com.edumind.ai.service.routing;

import com.edumind.ai.router.IntentRouter;

/**
 * 意图路由与执行编排：chat / rag / agent / navigate
 */
public interface IntentDispatchService {

    IntentRouter.IntentResult route(String message, Long courseId);

    IntentDispatchPlan prepare(IntentDispatchRequest request);
}
