package com.edumind;

import com.edumind.ai.controller.agent.AgentController;
import com.edumind.ai.controller.agent.AgentRunController;
import com.edumind.ai.controller.gateway.AiModelController;
import com.edumind.ai.controller.gateway.GatewayController;
import com.edumind.knowledge.controller.graph.KnowledgeGraphController;
import com.edumind.statistics.controller.analytics.AiUsageAnalyticsController;
import com.edumind.statistics.controller.analytics.KnowledgeMasteryController;
import com.edumind.statistics.controller.analytics.LearningAnalyticsController;
import com.edumind.statistics.controller.analytics.TeachingAdviceController;
import com.edumind.statistics.controller.analytics.WrongQuestionController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GateV10ArchitectureTest {

    @Test
    void v10AnalyticsControllersShouldBePresent() {
        Assertions.assertNotNull(LearningAnalyticsController.class);
        Assertions.assertNotNull(KnowledgeMasteryController.class);
        Assertions.assertNotNull(WrongQuestionController.class);
        Assertions.assertNotNull(TeachingAdviceController.class);
        Assertions.assertNotNull(AiUsageAnalyticsController.class);
    }

    @Test
    void v10GatewayAndAgentControllersShouldBePresent() {
        Assertions.assertNotNull(GatewayController.class);
        Assertions.assertNotNull(AiModelController.class);
        Assertions.assertNotNull(AgentController.class);
        Assertions.assertNotNull(AgentRunController.class);
        Assertions.assertNotNull(KnowledgeGraphController.class);
    }

    @Test
    void crossModulePublicApisShouldBePresent() {
        Assertions.assertNotNull(com.edumind.ai.api.AiAuditQueryApi.class);
        Assertions.assertNotNull(com.edumind.ai.api.QuestionGenerateApi.class);
    }
}
