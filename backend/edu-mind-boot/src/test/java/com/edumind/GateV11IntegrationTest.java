package com.edumind;

import com.edumind.ai.agent.executor.AgentExecutorImpl;
import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.dto.question.SmartPaperComposeDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.router.IntentRouter.IntentResult;
import com.edumind.ai.service.agent.AgentRunService;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.service.question.SmartPaperComposeService;
import com.edumind.ai.vo.agent.AgentRunVO;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.dto.graph.KnowledgePointRelationCreateDTO;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.vo.graph.KnowledgePointRelationVO;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * V1.1 智能中枢深化版 全链路集成测试验收套件
 */
@Tag("integration")
@EnabledIfSystemProperty(named = "gateH.integration", matches = "true")
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles({"test", "gateh"})
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
    private AgentRunService agentRunService;
    @Autowired
    private AgentExecutorImpl agentExecutor;
    @Autowired
    private com.edumind.statistics.service.analytics.LearningAnalyticsService learningAnalyticsService;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LoginUser user = LoginUser.builder().id(2L).username("teacher").build();
        UserContext.set(user);
        TenantContext.setTenantId(1L);
        // 依赖 migration + R__gate_e2e_seeds.sql 初始化，禁止 DROP 生产/共享表
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        TenantContext.clear();
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
        assertTrue(vo.getSelectedCount() <= 10, "Should not exceed requested count");
        assertNotNull(vo.getShortfallCount());
        if (vo.getShortfallCount() > 0) {
            assertTrue(vo.getSelectedCount() < 10, "Shortfall means fewer than requested questions");
        }

        assertNotNull(vo.getDifficultyHistogram());
        assertNotNull(vo.getTypeDistribution());
        assertNotNull(vo.getDuplicateRate());

        // 覆盖率：题池充足时应 >= 0.8；不足时必须有 shortfall 且无 synthetic id
        if (vo.getShortfallCount() != null && vo.getShortfallCount() > 0) {
            assertTrue(vo.getSelectedCount() < dto.getTotalCount());
        } else {
            assertTrue(vo.getCoverageRate() >= 0.8, "Coverage should be >= 0.8 when no shortfall");
        }
        vo.getQuestions().forEach(q -> assertTrue(q.getId() == null || q.getId() < 9000L,
                "Should not contain synthetic question ids"));
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

        Long expectedAiCalls = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_call_log WHERE course_id = ? AND create_time >= ?",
                Long.class, 102L, yesterday.atStartOfDay());
        assertNotNull(expectedAiCalls);
        assertEquals(expectedAiCalls.intValue(), entity.getAiCallCount() != null ? entity.getAiCallCount() : 0,
                "ai_call_count should match ai_call_log since stat date");

        var analyticsVO = learningAnalyticsService.getLearningAnalytics(102L, "7d", null);
        assertNotNull(analyticsVO);
        assertNotNull(analyticsVO.getTrends());
        assertFalse(analyticsVO.getTrends().getLearning().isEmpty());
        assertNotNull(analyticsVO.getAggregated());
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

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) heatmap.get("cells");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) heatmap.get("students");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kps = (List<Map<String, Object>>) heatmap.get("knowledgePoints");
        Set<Long> studentIds = students.stream().map(s -> ((Number) s.get("id")).longValue()).collect(Collectors.toSet());
        Set<Long> kpIds = kps.stream().map(k -> ((Number) k.get("id")).longValue()).collect(Collectors.toSet());
        for (Map<String, Object> cell : cells) {
            long sid = ((Number) cell.get("studentId")).longValue();
            long kpid = ((Number) cell.get("knowledgePointId")).longValue();
            double mastery = ((Number) cell.get("mastery")).doubleValue();
            assertTrue(studentIds.contains(sid));
            assertTrue(kpIds.contains(kpid));
            assertTrue(mastery >= 0.0 && mastery <= 1.0);
        }

        if (!cells.isEmpty()) {
            Map<String, Object> first = cells.get(0);
            Map<String, Object> drillDown = knowledgeMasteryService.getHeatmapCell(
                    102L,
                    ((Number) first.get("studentId")).longValue(),
                    ((Number) first.get("knowledgePointId")).longValue());
            assertNotNull(drillDown.get("mastery"));
        }
    }

    /**
     * 7. 知识图谱 AI 建边建议测试
     */
    @Test
    void testKnowledgeGraphSuggestRelations() {
        List<Map<String, Object>> suggestions = knowledgeGraphService.suggestRelations(2L, 1L, 5);
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        Map<String, Object> first = suggestions.get(0);
        assertTrue(first.containsKey("relationType"));
        assertTrue(first.containsKey("confidence"));
        assertTrue(first.containsKey("reason"));

        KnowledgePointRelationCreateDTO dto = new KnowledgePointRelationCreateDTO();
        dto.setTargetKnowledgePointId(((Number) first.get("targetKnowledgePointId")).longValue());
        dto.setRelationType(String.valueOf(first.get("relationType")));

        long targetId = dto.getTargetKnowledgePointId();
        String relationType = dto.getRelationType();
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_point_relation WHERE source_knowledge_point_id = ? AND target_knowledge_point_id = ? AND relation_type = ?",
                Integer.class, 1L, targetId, relationType);
        if (existing == null || existing == 0) {
            knowledgeGraphService.createRelation(1L, dto);
        }

        List<KnowledgePointRelationVO> relations = knowledgeGraphService.listRelations(1L);
        assertFalse(relations.isEmpty());
        assertTrue(relations.stream().anyMatch(r ->
                Long.valueOf(targetId).equals(r.getTargetKnowledgePointId())
                        && relationType.equals(r.getRelationType())));
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

    /**
     * 9. ReAct 多 Tool 动态执行（禁止 silent fallback）
     */
    @Test
    void testReactAgentMultiToolNoFallback() {
        String goal = "分析班级学情并推荐巩固资源";
        String runId = agentExecutor.createRun("teaching", 2L, 102L, goal);
        agentExecutor.executeRun(runId, "teaching", 102L, goal, null);

        AgentRunVO run = agentRunService.getRun(runId);
        assertNotNull(run);
        assertEquals("SUCCEEDED", run.getStatus(), "ReAct teaching run should succeed");
        assertNotNull(run.getResult());
        assertEquals("react", run.getResult().get("executionMode"));
        assertFalse(String.valueOf(run.getResult()).contains("switch_fallback"));

        List<String> tools = jdbcTemplate.query(
                "SELECT DISTINCT tool_name FROM agent_tool_call WHERE run_id = ? AND tool_name IS NOT NULL",
                (rs, row) -> rs.getString(1), runId);
        assertTrue(tools.size() >= 2, "ReAct should invoke at least 2 distinct tools: " + tools);
    }
}
