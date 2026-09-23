package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.support.InternalInvocationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 临时诊断用：复现「异步流水线切片静默失败」。
 * 模拟异步线程身份（无登录态 + 内部调用标记），触发切片，并打印触发前后的未提交事务与锁持有者。
 */
@SpringBootTest(classes = EduMindApplication.class,
        properties = "edumind.security.master-secret=EduMind_Memory_Key_Seed_2026")
@ActiveProfiles("test")
class ChunkPipelineReproTest {

    private static final Long DOC_ID = 647L;
    private static final Long TENANT_ID = 1L;

    @Autowired
    private ChunkService chunkService;
    @Autowired
    private KnowledgeDocumentChunkDao chunkDao;
    @Autowired
    private KnowledgeDocumentDao documentDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reproduceAsyncChunk() {
        TenantContext.setTenantId(TENANT_ID);
        try {
            dumpState("before");
            try {
                InternalInvocationContext.runInternal(() -> chunkService.triggerChunk(DOC_ID));
                System.out.println("[REPRO] chunk OK");
            } catch (Exception ex) {
                System.out.println("[REPRO] chunk FAILED: " + ex.getClass().getSimpleName()
                        + " | " + String.valueOf(ex.getMessage()).replace('\n', ' '));
                dumpState("after");
                throw ex;
            }
            dumpState("done");
        } finally {
            TenantContext.clear();
        }
    }

    private void dumpState(String tag) {
        try {
            long chunks = chunkDao.countByDocumentId(DOC_ID);
            KnowledgeDocumentEntity doc = documentDao.findById(DOC_ID);
            System.out.println("[REPRO:" + tag + "] chunks=" + chunks
                    + " status=" + (doc != null ? doc.getParseStatus() : "null")
                    + " err=" + (doc != null ? doc.getErrorMessage() : "null"));
        } catch (Exception ex) {
            System.out.println("[REPRO:" + tag + "] doc state failed: " + ex.getMessage());
        }
        try {
            jdbcTemplate.queryForList(
                    "SELECT trx_id, trx_state, trx_started, TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS age_sec, "
                            + "trx_mysql_thread_id AS thread, COALESCE(trx_query,'(idle)') AS q "
                            + "FROM information_schema.innodb_trx ORDER BY trx_started")
                    .forEach(row -> System.out.println("[REPRO:" + tag + "] TRX " + row));
        } catch (Exception ex) {
            System.out.println("[REPRO:" + tag + "] innodb_trx failed: " + ex.getMessage());
        }
        try {
            jdbcTemplate.queryForList(
                    "SELECT OBJECT_NAME, LOCK_TYPE, LOCK_MODE, LOCK_STATUS, LOCK_DATA "
                            + "FROM performance_schema.data_locks WHERE OBJECT_NAME='knowledge_document'")
                    .forEach(row -> System.out.println("[REPRO:" + tag + "] LOCK " + row));
        } catch (Exception ex) {
            System.out.println("[REPRO:" + tag + "] data_locks failed: " + ex.getMessage());
        }
        try {
            jdbcTemplate.queryForList(
                    "SELECT ID, USER, HOST, DB, COMMAND, TIME, STATE, LEFT(COALESCE(INFO,''), 90) AS info "
                            + "FROM information_schema.processlist WHERE COMMAND <> 'Sleep' OR TIME > 20")
                    .forEach(row -> System.out.println("[REPRO:" + tag + "] PROC " + row));
        } catch (Exception ex) {
            System.out.println("[REPRO:" + tag + "] processlist failed: " + ex.getMessage());
        }
    }
}
