package com.edumind;

import com.edumind.ai.dao.PromptTemplateDao;
import com.edumind.ai.dao.PromptTemplateVersionDao;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.entity.PromptTemplateVersionEntity;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.rag.retrieval.RagRetrieverImpl;
import com.edumind.ai.service.audit.TokenStatisticsService;
import com.edumind.ai.service.prompt.PromptManageService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.audit.TokenAuditSummaryVO;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.IndexStatusVO;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Gate F V0.5 集成验收（需本地 MySQL + Redis）。
 * 运行：mvn test -DgateF.integration=true -Dtest=GateFV05IntegrationTest
 * 默认 mvn test 不执行本类（见 @EnabledIfSystemProperty）。
 */
@Tag("integration")
@EnabledIfSystemProperty(named = "gateF.integration", matches = "true")
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
class GateFV05IntegrationTest {

    private static final Long TEACHER_ID = 2L;
    private static final Long GATE_F_KB_ID = 3L;
    private static final Long SEED_DOC_ID = 1L;
    private static final Long SEED_KB_ID = 1L;
    private static final String CHAT_RAG_ORIGINAL =
            "你是课程 AI 助教。请基于以下资料回答用户问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ChunkService chunkService;
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    @Autowired
    private KnowledgeBaseDao knowledgeBaseDao;
    @Autowired
    private RagRetrieverImpl ragRetriever;
    @Autowired
    private RagPipelineImpl ragPipeline;
    @Autowired
    private PromptTemplateDao promptTemplateDao;
    @Autowired
    private PromptTemplateVersionDao promptTemplateVersionDao;
    @Autowired
    private PromptManageService promptManageService;
    @Autowired
    private PromptService promptService;
    @Autowired
    private TokenStatisticsService tokenStatisticsService;
    @Autowired
    private KnowledgeAccessService knowledgeAccessService;
    @BeforeEach
    void assumeInfrastructureAndSeed() {
        assumeTrue(canConnectDatabase(), "MySQL edumind 不可用，跳过 Gate F 集成测试");
        TenantContext.setTenantId(1L);
        KnowledgeBaseEntity gateKb = knowledgeBaseDao.findById(GATE_F_KB_ID);
        assumeTrue(gateKb != null, "请先执行 sql/migration/R__gate_e2e_seeds.sql");
        loginAsTeacher();
    }

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
        TenantContext.clear();
    }

    @Test
    void documentPipeline_chunkAndIndex() {
        if (knowledgeDocumentChunkDao.countByDocumentId(SEED_DOC_ID) == 0) {
            chunkService.triggerChunk(SEED_DOC_ID);
        }
        assertTrue(knowledgeDocumentChunkDao.countByDocumentId(SEED_DOC_ID) > 0,
                "Chunk 数量应大于 0");

        indexingService.triggerIndex(SEED_KB_ID, "FULL");
        IndexStatusVO status = awaitIndexStatus(SEED_KB_ID, 60);
        assertTrue("INDEXED".equals(status.getStatus()) && status.getIndexedChunks() > 0,
                "索引应完成且已向量化 Chunk");
    }

    private void warmVectorStore() {
        if (knowledgeDocumentChunkDao.countByDocumentId(SEED_DOC_ID) == 0) {
            chunkService.triggerChunk(SEED_DOC_ID);
        }
        indexingService.triggerIndex(SEED_KB_ID, "FULL");
        awaitIndexStatus(SEED_KB_ID, 60);
    }

    @Test
    void retrieve_returnsTopKWithScore() {
        warmVectorStore();
        List<RetrievalHit> hits = ragRetriever.retrieveWithRewrittenQuery(
                "线性表", SEED_KB_ID, 5, 0.0, null);
        assertFalse(hits.isEmpty(), "检索结果不应为空");
        assertTrue(hits.get(0).getScore() >= 0, "应包含 score");
    }

    @Test
    void ragDebug_returnsPipelineStages() {
        warmVectorStore();
        RagResult result = ragPipeline.executeDetailed(
                "Java 多态有哪些实现方式？", SEED_KB_ID, 5, 0.0, null, true);
        assertNotNull(result.getOriginalQuery());
        assertNotNull(result.getRewrittenQuery());
        assertNotNull(result.getContext());
        assertNotNull(result.getPromptPreview());
        assertFalse(result.getPromptPreview().isBlank());
    }

    @Test
    void ragPipeline_returnsRetrievalHitsForCitation() {
        warmVectorStore();
        RagResult result = ragPipeline.executeDetailed(
                "线性表", SEED_KB_ID, 5, 0.0, null, true);
        assertFalse(result.getRetrievalResults().isEmpty(),
                "RAG 应返回可用于 citation 的检索命中");
    }

    @Test
    void promptPublishAndRollback_affectRender() {
        resetChatRagTemplate();
        PromptTemplateEntity template = promptTemplateDao.findByCode("chat_rag");
        assumeTrue(template != null, "缺少 chat_rag 种子模板");
        int versionBeforePublish = 1;
        ensurePromptVersionSnapshot(template.getId(), versionBeforePublish, CHAT_RAG_ORIGINAL, template.getVariables());
        String marker = "GATE_F_MARKER_" + System.currentTimeMillis();
        try {
            template.setContent(CHAT_RAG_ORIGINAL + "\n" + marker);
            promptTemplateDao.updateById(template);
            promptManageService.publish(template.getId());

            String rendered = promptService.renderTemplate("chat_rag",
                    Map.of("context", "ctx", "question", "q"));
            assertTrue(rendered.contains(marker), "发布后 render 应包含新内容");

            promptManageService.rollback(template.getId(), versionBeforePublish);
            rendered = promptService.renderTemplate("chat_rag",
                    Map.of("context", "ctx", "question", "q"));
            assertFalse(rendered.contains(marker), "回滚后 render 不应包含新版本标记");
        } finally {
            resetChatRagTemplate();
        }
    }

    @Test
    void tokenSummary_last7Days() {
        TokenAuditSummaryVO summary = tokenStatisticsService.summary(
                "day", LocalDate.now().minusDays(6), LocalDate.now());
        assertNotNull(summary.getDailyTrend());
        assertEquals(7, summary.getDailyTrend().size(), "dailyTrend 应覆盖 7 天");
        assertTrue(summary.getTotalCalls() >= 0);
    }

    @Test
    void knowledgeAccess_deniesCrossTeacher() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> knowledgeAccessService.assertAccessible(GATE_F_KB_ID));
        assertTrue(ex.getMessage().contains("无权") || ex.getMessage().contains("知识库"),
                "teacher 不应访问 teacher2 专属知识库");
    }

    private void resetChatRagTemplate() {
        PromptTemplateEntity template = promptTemplateDao.findByCode("chat_rag");
        if (template == null) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM prompt_template_version WHERE template_id = ? AND version > 1",
                template.getId());
        template.setContent(CHAT_RAG_ORIGINAL);
        template.setVariables("context,question");
        template.setVersion(1);
        template.setStatus("PUBLISHED");
        promptTemplateDao.updateById(template);
        promptService.evictTemplate("chat_rag");
    }

    private IndexStatusVO awaitIndexStatus(Long knowledgeBaseId, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        IndexStatusVO status = indexingService.getIndexStatus(knowledgeBaseId);
        while (System.currentTimeMillis() < deadline) {
            if ("INDEXED".equals(status.getStatus())
                    || "INDEX_FAILED".equals(status.getStatus())) {
                break;
            }
            sleepQuietly(500);
            status = indexingService.getIndexStatus(knowledgeBaseId);
        }
        return status;
    }

    private void loginAsTeacher() {
        UserContext.set(LoginUser.builder()
                .id(TEACHER_ID)
                .username("teacher")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .build());
    }

    private boolean canConnectDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensurePromptVersionSnapshot(Long templateId, int version, String content, String variables) {
        if (promptTemplateVersionDao.findByTemplateIdAndVersion(templateId, version) != null) {
            return;
        }
        PromptTemplateVersionEntity snapshot = new PromptTemplateVersionEntity();
        snapshot.setTemplateId(templateId);
        snapshot.setVersion(version);
        snapshot.setContent(content);
        snapshot.setVariables(variables);
        snapshot.setPublishedBy(TEACHER_ID);
        promptTemplateVersionDao.insert(snapshot);
    }
}
