package com.edumind;

import com.edumind.ai.controller.prompt.PromptController;
import com.edumind.ai.controller.tool.LessonPlanController;
import com.edumind.ai.controller.tool.SummaryController;
import com.edumind.knowledge.controller.graph.KnowledgeGraphController;
import com.edumind.statistics.controller.ai.AiRecommendationController;
import com.edumind.statistics.controller.learning.LearningPathController;
import com.edumind.statistics.controller.teaching.TeachingReportController;
import com.edumind.system.controller.user.UserPreferenceController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Gate F V0.5 架构验收：新增 Controller 已注册（不依赖外部运行时）。
 */
class GateFV05ArchitectureTest {

    @Test
    void v05ControllersShouldBePresent() {
        Assertions.assertNotNull(PromptController.class);
        Assertions.assertNotNull(LessonPlanController.class);
        Assertions.assertNotNull(SummaryController.class);
        Assertions.assertNotNull(KnowledgeGraphController.class);
        Assertions.assertNotNull(AiRecommendationController.class);
        Assertions.assertNotNull(LearningPathController.class);
        Assertions.assertNotNull(TeachingReportController.class);
        Assertions.assertNotNull(UserPreferenceController.class);
    }
}
