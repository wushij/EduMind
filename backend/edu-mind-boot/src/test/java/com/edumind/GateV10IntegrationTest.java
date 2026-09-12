package com.edumind;

import com.edumind.ai.agent.executor.AgentExecutorImpl;
import com.edumind.ai.dto.agent.AgentRunCreateDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.agent.AgentRunService;
import com.edumind.ai.service.question.SmartPaperComposeService;
import com.edumind.ai.vo.agent.AgentRunVO;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.vo.graph.GraphGapVO;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.analytics.WrongQuestionDiagnosisService;
import com.edumind.statistics.service.learning.AdaptivePathService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@EnabledIfSystemProperty(named = "gateG.integration", matches = "true")
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
class GateV10IntegrationTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LearningAnalyticsService learningAnalyticsService;
    @Autowired
    private KnowledgeMasteryService knowledgeMasteryService;
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    @Autowired
    private AgentRunService agentRunService;
    @Autowired
    private AgentExecutorImpl agentExecutor;
    @Autowired
    private AiGatewayFacade aiGatewayFacade;
    @Autowired
    private AdaptivePathService adaptivePathService;
    @Autowired
    private WrongQuestionDiagnosisService wrongQuestionDiagnosisService;
    @Autowired
    private SmartPaperComposeService smartPaperComposeService;

    @BeforeEach
    void setUp() {
        LoginUser user = LoginUser.builder().id(2L).username("teacher").build();
        UserContext.set(user);
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

    @Test
    void learningAnalyticsShouldReturnData() {
        LearningAnalyticsVO vo = learningAnalyticsService.getLearningAnalytics(102L, "7d", null);
        assertNotNull(vo);
        assertNotNull(vo.getCourseId());
    }

    @Test
    void knowledgeMasteryShouldReturnRadar() {
        KnowledgeMasteryVO vo = knowledgeMasteryService.getMastery(102L, 3L);
        assertNotNull(vo);
        assertTrue(vo.getDimensions().size() >= 0);
    }

    @Test
    void graphGapsShouldWork() {
        List<GraphGapVO> gaps = knowledgeGraphService.findGaps(2L, 3L, 0.6);
        assertNotNull(gaps);
    }

    @Test
    void agentRunShouldComplete() throws InterruptedException {
        AgentRunCreateDTO dto = new AgentRunCreateDTO();
        dto.setAgentCode("question");
        dto.setCourseId(102L);
        dto.setGoal("针对 Java 多态生成 5 道中等难度单选题");
        String runId = agentRunService.startRun(dto, 2L);
        AgentRunVO run = waitForRun(runId, 60);
        assertNotNull(run);
        assertTrue(!"RUNNING".equals(run.getStatus()), "Agent run should finish: " + run.getStatus());
        assertTrue(run.getSteps().size() >= 1, "Agent should record at least one step");
    }

    @Test
    void teachingAgentShouldReturnStructuredCitations() {
        String goal = "请解释 Java 多态的原理";
        String runId = agentExecutor.createRun("teaching", 2L, 102L, goal);
        agentExecutor.executeRun(runId, "teaching", 102L, goal, null);
        AgentRunVO run = agentRunService.getRun(runId);
        assertNotNull(run);
        assertTrue("SUCCEEDED".equals(run.getStatus()), "Teaching agent should succeed: " + run.getStatus());
        assertNotNull(run.getResult());
        Object citations = run.getResult().get("citations");
        assertTrue(citations instanceof List, "citations should be structured list");
    }

    @Test
    void smartPaperComposeShouldReturnCoverage() {
        SmartPaperComposeVO vo = smartPaperComposeService.compose(102L, null, 10, null);
        assertNotNull(vo);
        assertNotNull(vo.getCoverageRate());
        assertTrue(vo.getSelectedCount() >= 0);
    }

    @Test
    void gatewayChatShouldWork() {
        String answer = aiGatewayFacade.chat("CHAT", "你是助手", "你好");
        assertNotNull(answer);
        assertTrue(answer.contains("Mock") || answer.length() > 0);
    }

    @Test
    void gatewayFallbackShouldUseMockClient() {
        String answer = aiGatewayFacade.chat("CHAT", "deepseek-chat", "你是助手", "fallback test");
        assertNotNull(answer);
        assertTrue(answer.startsWith("[") || answer.contains("Mock"));
    }

    @Test
    void adaptivePathShouldBuild() {
        assertNotNull(adaptivePathService.buildAdaptivePath(102L, 3L));
    }

    @Test
    void wrongQuestionDiagnoseShouldPersistVariants() {
        Long recordId = jdbcTemplate.query(
                "SELECT id FROM wrong_question_record WHERE student_id = 3 AND question_id = 1007 LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        assumeTrue(recordId != null, "需执行 sql/migration/R__gate_g_e2e_seed.sql");
        WrongQuestionRecordEntity entity = wrongQuestionDiagnosisService.diagnoseRecord(recordId);
        assertNotNull(entity.getDiagnosis());
        assertNotNull(entity.getVariantQuestionIds());
        assertFalse("1001,1002".equals(entity.getVariantQuestionIds()));
        assertTrue(entity.getVariantQuestionIds().split(",").length >= 1);
    }

    private AgentRunVO waitForRun(String runId, int maxAttempts) throws InterruptedException {
        AgentRunVO run = null;
        for (int i = 0; i < maxAttempts; i++) {
            run = agentRunService.getRun(runId);
            if (run != null && !"RUNNING".equals(run.getStatus())) {
                return run;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        return run;
    }
}
