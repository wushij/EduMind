package com.edumind;

import com.edumind.ai.controller.audit.TokenStatisticsController;
import com.edumind.ai.controller.prompt.PromptController;
import com.edumind.ai.controller.quota.AiQuotaController;
import com.edumind.ai.controller.rag.RagDebugController;
import com.edumind.ai.controller.rag.RetrievalController;
import com.edumind.ai.controller.tool.SummaryController;
import com.edumind.knowledge.controller.chunk.ChunkController;
import com.edumind.knowledge.controller.graph.KnowledgeGraphController;
import com.edumind.knowledge.controller.index.KnowledgeIndexController;
import com.edumind.statistics.controller.ai.AiRecommendationController;
import com.edumind.statistics.controller.learning.LearningPathController;
import com.edumind.statistics.controller.teaching.TeachingReportController;
import com.edumind.system.controller.user.UserPreferenceController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Gate F V0.5 架构验收：核心 Controller 已注册（不依赖外部运行时）。
 * 完整链路验收见 {@link GateFV05IntegrationTest}（需 -DgateF.integration=true + MySQL/Redis）。
 */
class GateFV05ArchitectureTest {

    @Test
    void v05CoreControllersShouldBePresent() {
        Assertions.assertNotNull(ChunkController.class);
        Assertions.assertNotNull(KnowledgeIndexController.class);
        Assertions.assertNotNull(RetrievalController.class);
        Assertions.assertNotNull(RagDebugController.class);
        Assertions.assertNotNull(PromptController.class);
        Assertions.assertNotNull(TokenStatisticsController.class);
        Assertions.assertNotNull(AiQuotaController.class);
    }

    @Test
    void v05ExtensionControllersShouldBePresent() {
        Assertions.assertNotNull(SummaryController.class);
        Assertions.assertNotNull(KnowledgeGraphController.class);
        Assertions.assertNotNull(AiRecommendationController.class);
        Assertions.assertNotNull(LearningPathController.class);
        Assertions.assertNotNull(TeachingReportController.class);
        Assertions.assertNotNull(UserPreferenceController.class);
    }
}
