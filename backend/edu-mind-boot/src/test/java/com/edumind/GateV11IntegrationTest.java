package com.edumind;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.dto.question.SmartPaperComposeDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.router.IntentRouter.IntentResult;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.question.SmartPaperComposeService;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.job.CourseStatisticsJob;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * V1.1 智能中枢深化版 全链路集成测试验收套件
 */
@Tag("integration")
@EnabledIfSystemProperty(named = "gateH.integration", matches = "true")
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class GateV11IntegrationTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private IntentRouter intentRouter;
    @Autowired
    private GlobalAssistantService globalAssistantService;
    @Autowired
    private AiGatewayFacade aiGatewayFacade;
    @Autowired
    private SmartPaperComposeService smartPaperComposeService;
    @Autowired
    private CourseStatisticsJob courseStatisticsJob;
    @Autowired
    private KnowledgeMasteryService knowledgeMasteryService;
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    @Autowired
    private List<AgentTool> agentTools;
    @Autowired
    private com.edumind.statistics.service.analytics.LearningAnalyticsService learningAnalyticsService;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LoginUser user = LoginUser.builder().id(2L).username("teacher").build();
        UserContext.set(user);

        try {
            jdbcTemplate.execute("ALTER TABLE ai_call_log ADD COLUMN course_id BIGINT NULL COMMENT '关联课程ID'");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE ai_call_log ADD INDEX idx_course_create (course_id, create_time)");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS `course_statistics`");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `course_statistics` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `course_id` BIGINT NOT NULL,
                  `stat_date` DATE NOT NULL,
                  `student_count` INT NOT NULL DEFAULT 0,
                  `avg_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                  `mastery_avg` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                  `ai_call_count` INT NOT NULL DEFAULT 0,
                  `wrong_count` INT NOT NULL DEFAULT 0,
                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_course_date` (`course_id`, `stat_date`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        } catch (Exception ignored) {}
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void databaseShouldBeAvailable() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assumeTrue(conn.isValid(2));
        }
    }

    /**
     * 1. 意图分发路由测试
     */
    @Test
    void testIntentRouterClassification() {
        IntentResult examIntent = intentRouter.route("帮我出一份包含导数与微分的期中试卷", 102L);
        assertNotNull(examIntent);
        assertEquals("agent", examIntent.type());
        assertEquals("exam", examIntent.targetCode());

        IntentResult ragIntent = intentRouter.route("请检索微积分第一章的核心切片和知识点资料", 102L);
        assertNotNull(ragIntent);
        assertEquals("rag", ragIntent.type());

        IntentResult navIntent = intentRouter.route("我想看看班级的学情分析报表", 102L);
        assertNotNull(navIntent);
        assertEquals("navigate", navIntent.type());
    }

    /**
     * 2. 全局助理问答统一入口测试
     */
    @Test
    void testGlobalAssistantAsk() {
        GlobalAssistantRequestDTO dto = new GlobalAssistantRequestDTO();
        dto.setMessage("帮我针对微分方程出一套难度适中的练习卷");
        dto.setCourseId(102L);

        Map<String, Object> vo = globalAssistantService.ask(dto);
        assertNotNull(vo);
        assertNotNull(vo.get("intent"));
        assertNotNull(vo.get("intentDesc"));
        assertNotNull(vo.get("content"));
        assertEquals("/ai/exam/generate", vo.get("targetCode"));
    }

    /**
     * 3. AI Gateway 生产级保障 (降级 Fallback / 令牌桶限流 429 / 熔断度量)
     */
    @Test
    void testAiGatewayResilienceAndMetrics() {
        // 1. 验证 Stream 接口正常执行
        assertDoesNotThrow(() -> {
            aiGatewayFacade.streamChat("chat", "你是一个助教", "你好", chunk -> {});
        });

        // 2. 真实令牌桶限流校验：突发超过阈值必须触发 429 (TOO_MANY_REQUESTS)
        assertThrows(com.edumind.common.exception.BusinessException.class, () -> {
            for (int i = 0; i < 150; i++) {
                aiGatewayFacade.checkRateLimit("stress-test-scene", 100);
            }
        }, "Rate limiter should throw 429 when exceeding threshold");

        // 3. 熔断状态机验证：开启熔断后直接进入 Fallback
        aiGatewayFacade.forceOpenCircuit();
        assertTrue(aiGatewayFacade.isCircuitOpen(), "Circuit breaker should be OPEN");
        aiGatewayFacade.resetCircuit();
        assertFalse(aiGatewayFacade.isCircuitOpen(), "Circuit breaker should be CLOSED after reset");

        // 4. 指标快照验证
        AiGatewayFacade.GatewayMetrics metrics = aiGatewayFacade.snapshot();
        assertNotNull(metrics);
        assertTrue(metrics.getTotalRequests() > 0);
        assertTrue(metrics.getRateLimitedCount() > 0);
        assertTrue(metrics.getCircuitOpenCount() > 0);
    }

    /**
     * 4. 智能组卷算法 v2 测试 (多目标约束：难度分布、题型比例、覆盖率计算)
     */
    @Test
    void testSmartPaperComposeV2() {
        SmartPaperComposeDTO dto = new SmartPaperComposeDTO();
        dto.setCourseId(102L);
        dto.setTotalCount(10);
        dto.setTotalScore(100);
        dto.setDifficultyDistribution(Map.of("EASY", 0.3, "MEDIUM", 0.5, "HARD", 0.2));
        dto.setTypeRatios(Map.of("SINGLE_CHOICE", 0.6, "JUDGE", 0.4));

        SmartPaperComposeVO vo = smartPaperComposeService.composeV2(dto);
        assertNotNull(vo);
        assertNotNull(vo.getQuestions());
        assertEquals(10, vo.getSelectedCount(), "Should select exactly 10 questions");

        // 严格断言难度正态分布 (3:5:2)
        assertNotNull(vo.getDifficultyHistogram());
        assertEquals(3, vo.getDifficultyHistogram().get("EASY"), "EASY should match 30% of 10 = 3");
        assertEquals(5, vo.getDifficultyHistogram().get("MEDIUM"), "MEDIUM should match 50% of 10 = 5");
        assertEquals(2, vo.getDifficultyHistogram().get("HARD"), "HARD should match 20% of 10 = 2");

        // 严格断言题型比例 (6:4)
        assertNotNull(vo.getTypeDistribution());
        assertEquals(6, vo.getTypeDistribution().get("SINGLE_CHOICE"), "SINGLE_CHOICE should be 60% of 10 = 6");
        assertEquals(4, vo.getTypeDistribution().get("JUDGE"), "JUDGE should be 40% of 10 = 4");

        // 覆盖率大于 0
        assertTrue(vo.getCoverageRate() > 0.0);
        assertEquals(100.0, vo.getTotalScore());
    }

    /**
     * 5. 学情日聚合统计 Job 与报表预聚合读取测试
     */
    @Test
    void testCourseStatisticsAggregationJob() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CourseStatisticsEntity entity = courseStatisticsJob.aggregateCourseStat(102L, yesterday);
        assertNotNull(entity);
        assertEquals(102L, entity.getCourseId());
        assertEquals(yesterday, entity.getStatDate());
        assertNotNull(entity.getStudentCount());
        assertNotNull(entity.getAvgScore());

        // 验证 LearningAnalytics 读取预聚合表
        var analyticsVO = learningAnalyticsService.getLearningAnalytics(102L, "7d", null);
        assertNotNull(analyticsVO);
        assertNotNull(analyticsVO.getTrends());
        assertFalse(analyticsVO.getTrends().getLearning().isEmpty());
    }

    /**
     * 6. 掌握度热力矩阵测试 (学生 × 考点)
     */
    @Test
    void testKnowledgeMasteryHeatmap() {
        Map<String, Object> heatmap = knowledgeMasteryService.getHeatmap(102L, "30d");
        assertNotNull(heatmap);
        assertEquals(102L, heatmap.get("courseId"));
        assertTrue(heatmap.containsKey("knowledgePoints"));
        assertTrue(heatmap.containsKey("students"));
        assertTrue(heatmap.containsKey("cells"));
    }

    /**
     * 7. 知识图谱 AI 建边建议测试
     */
    @Test
    void testKnowledgeGraphSuggestRelations() {
        List<Map<String, Object>> suggestions = knowledgeGraphService.suggestRelations(2L, 1L, 5);
        assertNotNull(suggestions);
    }

    /**
     * 8. ReAct Agent 工具扩展与执行测试
     */
    @Test
    void testAgentTools() {
        assertNotNull(agentTools);
        assertTrue(agentTools.size() >= 4, "Should register at least 4 AgentTool implementations");

        var profileToolOpt = agentTools.stream().filter(t -> "get_student_profile".equals(t.name())).findFirst();
        assertTrue(profileToolOpt.isPresent(), "get_student_profile tool must be registered");

        // 执行一次 get_student_profile 测试真实数据流
        var tool = profileToolOpt.get();
        Object result = tool.execute(Map.of("studentId", 1001L, "courseId", 102L));
        assertNotNull(result);
        String resultStr = String.valueOf(result);
        assertTrue(resultStr.contains("studentId") || resultStr.contains("name") || resultStr.contains("1001"));
    }
}
