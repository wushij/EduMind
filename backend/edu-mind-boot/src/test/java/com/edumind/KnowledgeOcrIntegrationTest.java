package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.dao.ocr.KnowledgeOcrDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrPageEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrTaskEntity;
import com.edumind.knowledge.integration.ocr.impl.MockOcrEngineAdapter;
import com.edumind.knowledge.mapper.KnowledgeDocumentMapper;
import com.edumind.knowledge.mapper.ocr.KnowledgeOcrPageMapper;
import com.edumind.knowledge.mapper.ocr.KnowledgeOcrTaskMapper;
import com.edumind.knowledge.service.ocr.KnowledgeOcrService;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 智教云 V2.0 · Knowledge OCR 异步任务编排全链路闭环集成测试 (Gate I6 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class KnowledgeOcrIntegrationTest {

    private static final Long TENANT_A = 9920L;
    private static final Long TENANT_B = 9921L;
    private static final Long USER_TEACHER = 99201L;

    @Autowired
    private KnowledgeOcrService ocrService;

    @Autowired
    private KnowledgeOcrDao ocrDao;

    @Autowired
    private KnowledgeOcrTaskMapper taskMapper;

    @Autowired
    private KnowledgeOcrPageMapper pageMapper;

    @Autowired
    private KnowledgeDocumentDao documentDao;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private KnowledgeBaseDao knowledgeBaseDao;

    @Autowired
    private KnowledgeDocumentTextDao documentTextDao;

    private Long kbIdA;
    private Long docIdA;
    private Long kbIdB;
    private Long docIdB;

    @BeforeEach
    public void setUp() {
        MockOcrEngineAdapter.setForceFail(false);
        MockOcrEngineAdapter.setMockDelayMs(50); // 加速测试

        cleanTestData();

        // 1. 初始化 Tenant A 知识库与文档
        TenantContext.setTenantId(TENANT_A);
        KnowledgeBaseEntity kbA = new KnowledgeBaseEntity();
        kbA.setTenantId(TENANT_A);
        kbA.setName("GateI6高等数学知识库_A");
        kbA.setDescription("测试用高数知识库");
        kbA.setCourseId(99201L);
        kbA.setDocCount(1);
        kbA.setChunkCount(0);
        kbA.setIndexStatus("IDLE");
        kbA.setStatus(1);
        knowledgeBaseDao.insert(kbA);
        kbIdA = kbA.getId();

        KnowledgeDocumentEntity docA = new KnowledgeDocumentEntity();
        docA.setKnowledgeBaseId(kbIdA);
        docA.setFileName("高等数学开学冲刺调研测试_A.pdf");
        docA.setFileType("pdf");
        docA.setFileSize(102400L);
        docA.setObjectKey("test/gate_i6_doc_a.pdf");
        docA.setParseStatus("PENDING");
        docA.setStatus(1);
        documentDao.insert(docA);
        docIdA = docA.getId();

        // 2. 初始化 Tenant B 知识库与文档 (用于跨租户 IDOR 测试)
        TenantContext.setTenantId(TENANT_B);
        KnowledgeBaseEntity kbB = new KnowledgeBaseEntity();
        kbB.setTenantId(TENANT_B);
        kbB.setName("GateI6大学物理知识库_B");
        kbB.setDescription("测试用物理知识库");
        kbB.setCourseId(99202L);
        kbB.setDocCount(1);
        kbB.setChunkCount(0);
        kbB.setIndexStatus("IDLE");
        kbB.setStatus(1);
        knowledgeBaseDao.insert(kbB);
        kbIdB = kbB.getId();

        KnowledgeDocumentEntity docB = new KnowledgeDocumentEntity();
        docB.setKnowledgeBaseId(kbIdB);
        docB.setFileName("大学物理期末测验_B.pdf");
        docB.setFileType("pdf");
        docB.setFileSize(204800L);
        docB.setObjectKey("test/gate_i6_doc_b.pdf");
        docB.setParseStatus("PENDING");
        docB.setStatus(1);
        documentDao.insert(docB);
        docIdB = docB.getId();

        // 默认将上下文设为 Tenant A
        TenantContext.setTenantId(TENANT_A);
        LoginUser user = LoginUser.builder()
                .id(USER_TEACHER)
                .username("gateI6_teacher")
                .roles(List.of(SecurityConstant.ROLE_ADMIN))
                .permissions(List.of("knowledge:ocr:use"))
                .build();
        UserContext.set(user);
    }

    @AfterEach
    public void tearDown() {
        MockOcrEngineAdapter.setForceFail(false);
        MockOcrEngineAdapter.setMockDelayMs(150);
        cleanTestData();
        TenantContext.clear();
        UserContext.clear();
    }

    private void cleanTestData() {
        TenantContext.clear();
        // 清理 Tenant A 与 Tenant B 的测试任务与文档
        List<KnowledgeOcrTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<KnowledgeOcrTaskEntity>()
                .in(KnowledgeOcrTaskEntity::getTenantId, List.of(TENANT_A, TENANT_B)));
        for (KnowledgeOcrTaskEntity task : tasks) {
            pageMapper.delete(new LambdaQueryWrapper<KnowledgeOcrPageEntity>()
                    .eq(KnowledgeOcrPageEntity::getTaskId, task.getId()));
            taskMapper.deleteById(task.getId());
        }

        if (docIdA != null) {
            documentTextDao.deleteByDocumentId(docIdA);
            documentMapper.deleteById(docIdA);
        }
        if (docIdB != null) {
            documentTextDao.deleteByDocumentId(docIdB);
            documentMapper.deleteById(docIdB);
        }
        if (kbIdA != null) {
            knowledgeBaseDao.deleteById(kbIdA);
        }
        if (kbIdB != null) {
            knowledgeBaseDao.deleteById(kbIdB);
        }
    }

    private boolean awaitCondition(BooleanSupplier condition, long timeoutMs, long intervalMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Test
    @DisplayName("用例1: 创建 OCR 任务立即返回 PENDING 初始态，禁止同步插入 pages")
    public void testCreateOcrTask_ReturnsPendingWithoutPages() {
        TenantContext.setTenantId(TENANT_A);

        KnowledgeOcrTaskVO vo = ocrService.createOcrTask(docIdA, "MINERU");

        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getId());
        Assertions.assertEquals(TENANT_A, vo.getTenantId());
        Assertions.assertEquals(docIdA, vo.getDocumentId());
        Assertions.assertEquals("PENDING", vo.getStatus());
        Assertions.assertEquals(0, vo.getProgress());

        // 验证调用瞬间，数据库中不存在任何该任务的 pages 记录 (彻底杜绝同步假完成)
        List<KnowledgeOcrPageEntity> pages = pageMapper.selectList(new LambdaQueryWrapper<KnowledgeOcrPageEntity>()
                .eq(KnowledgeOcrPageEntity::getTaskId, vo.getId()));
        Assertions.assertTrue(pages.isEmpty(), "创建 OCR 任务时严禁同步插入页面记录");
    }

    @Test
    @DisplayName("用例2: 异步 Dispatcher 派发后状态机流转至 PROOFREADING 并成功提取识别页")
    public void testAsyncDispatch_ReachesProofreadingWithPages() {
        TenantContext.setTenantId(TENANT_A);

        KnowledgeOcrTaskVO vo = ocrService.createOcrTask(docIdA, "PADDLE_OCR");
        Long taskId = vo.getId();

        // 轮询等待异步任务流转至 PROOFREADING
        boolean reached = awaitCondition(() -> {
            KnowledgeOcrTaskEntity t = taskMapper.selectById(taskId);
            return t != null && "PROOFREADING".equals(t.getStatus());
        }, 5000, 100);

        Assertions.assertTrue(reached, "OCR 异步任务未在预期时间内流转至 PROOFREADING 校对状态");

        // 验证状态与页数
        KnowledgeOcrTaskVO finalStatus = ocrService.getTaskStatus(taskId);
        Assertions.assertEquals("PROOFREADING", finalStatus.getStatus());
        Assertions.assertEquals(3, finalStatus.getTotalPages());
        Assertions.assertEquals(3, finalStatus.getProcessedPages());
        Assertions.assertEquals(100, finalStatus.getProgress());

        // 验证识别页列表
        List<KnowledgeOcrPageVO> pages = ocrService.getTaskPages(taskId);
        Assertions.assertEquals(3, pages.size());
        Assertions.assertTrue(pages.get(0).getRawText().contains("极限论基础"));
        Assertions.assertFalse(pages.get(0).getProofreadStatus());
    }

    @Test
    @DisplayName("用例3: 人工校对草稿 updatePageText 正确持久化与标记状态")
    public void testUpdatePageText_PersistedSuccessfully() {
        TenantContext.setTenantId(TENANT_A);

        KnowledgeOcrTaskVO vo = ocrService.createOcrTask(docIdA, "MINERU");
        Long taskId = vo.getId();

        awaitCondition(() -> {
            KnowledgeOcrTaskEntity t = taskMapper.selectById(taskId);
            return t != null && "PROOFREADING".equals(t.getStatus());
        }, 5000, 100);

        List<KnowledgeOcrPageVO> pages = ocrService.getTaskPages(taskId);
        Assertions.assertFalse(pages.isEmpty());
        Long pageId = pages.get(0).getId();

        // 人工修改校对文本
        String updatedProofreadText = "【教师人工深度校对】：定义 1.1（极限的存在准则）已核实公式准确。";
        ocrService.updatePageText(pageId, updatedProofreadText);

        // 再次获取页面确认持久化
        List<KnowledgeOcrPageVO> reloadedPages = ocrService.getTaskPages(taskId);
        KnowledgeOcrPageVO p1 = reloadedPages.stream().filter(p -> p.getId().equals(pageId)).findFirst().orElseThrow();
        Assertions.assertEquals(updatedProofreadText, p1.getProofreadText());
        Assertions.assertTrue(p1.getProofreadStatus(), "人工校对后状态必须更新为 true");
    }

    @Test
    @DisplayName("用例4: confirmAndIngest 确认校对后任务转 COMPLETED 并写入文档全文本与成功解析状态")
    public void testConfirmAndIngest_FullPipelineSuccess() {
        TenantContext.setTenantId(TENANT_A);

        KnowledgeOcrTaskVO vo = ocrService.createOcrTask(docIdA, "MINERU");
        Long taskId = vo.getId();

        awaitCondition(() -> {
            KnowledgeOcrTaskEntity t = taskMapper.selectById(taskId);
            return t != null && "PROOFREADING".equals(t.getStatus());
        }, 5000, 100);

        // 修改第一页校对文本
        List<KnowledgeOcrPageVO> pages = ocrService.getTaskPages(taskId);
        ocrService.updatePageText(pages.get(0).getId(), "校对后的极限核心考点文本内容");

        // 确认入库
        ocrService.confirmAndIngest(taskId);

        // 验证任务状态
        KnowledgeOcrTaskVO taskVO = ocrService.getTaskStatus(taskId);
        Assertions.assertEquals("COMPLETED", taskVO.getStatus());

        // 验证文档解析状态更新为 SUCCESS
        KnowledgeDocumentEntity updatedDoc = documentDao.findById(docIdA);
        Assertions.assertEquals("SUCCESS", updatedDoc.getParseStatus());

        // 验证全文本内容写入
        KnowledgeDocumentTextEntity text = documentTextDao.findByDocumentId(docIdA);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.getContent().contains("校对后的极限核心考点文本内容"));
    }

    @Test
    @DisplayName("用例5: confirmAndIngest 非 PROOFREADING 状态下调用必须 Fail-Closed 抛 400")
    public void testConfirmAndIngest_InvalidState_ThrowsBadRequest() {
        TenantContext.setTenantId(TENANT_A);

        // 构造一个刚创建处于 PENDING 的任务
        KnowledgeOcrTaskEntity pendingTask = new KnowledgeOcrTaskEntity();
        pendingTask.setTenantId(TENANT_A);
        pendingTask.setDocumentId(docIdA);
        pendingTask.setEngine("MINERU");
        pendingTask.setTotalPages(0);
        pendingTask.setProcessedPages(0);
        pendingTask.setStatus("PENDING");
        taskMapper.insert(pendingTask);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> {
            ocrService.confirmAndIngest(pendingTask.getId());
        });
        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), ex.getCode());
        Assertions.assertTrue(ex.getMessage().contains("当前任务状态不是校对中"));
    }

    @Test
    @DisplayName("用例6: 跨租户 IDOR 严格越权防护 (Tenant B 访问 Tenant A 任务与文档一律 403)")
    public void testCrossTenantAccess_ThrowsForbidden() {
        // 1. Tenant A 创建任务
        TenantContext.setTenantId(TENANT_A);
        KnowledgeOcrTaskVO voA = ocrService.createOcrTask(docIdA, "MINERU");
        Long taskIdA = voA.getId();

        // 2. 切换至 Tenant B 尝试越权访问 Tenant A 的任务
        TenantContext.setTenantId(TENANT_B);
        LoginUser userB = LoginUser.builder()
                .id(99202L)
                .username("gateI6_teacher_b")
                .permissions(List.of("knowledge:ocr:use"))
                .build();
        UserContext.set(userB);

        // 尝试查看 Tenant A 的任务状态
        BusinessException ex1 = Assertions.assertThrows(BusinessException.class, () -> {
            ocrService.getTaskStatus(taskIdA);
        });
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex1.getCode());

        // 尝试查看 Tenant A 的页面详情
        BusinessException ex2 = Assertions.assertThrows(BusinessException.class, () -> {
            ocrService.getTaskPages(taskIdA);
        });
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex2.getCode());

        // 尝试用 Tenant B 身份对 Tenant A 的文档发起 OCR 任务
        BusinessException ex3 = Assertions.assertThrows(BusinessException.class, () -> {
            ocrService.createOcrTask(docIdA, "MINERU");
        });
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex3.getCode());
        Assertions.assertTrue(ex3.getMessage().contains("无权访问其他租户的文档"));
    }

    @Test
    @DisplayName("用例7: 异常故障路径 (Mock 引擎异常时异步状态机正确标记 FAILED 并记录错误日志)")
    public void testAsyncDispatch_FailurePath_MarksTaskFailed() {
        TenantContext.setTenantId(TENANT_A);

        // 开启模拟引擎故障开关
        MockOcrEngineAdapter.setForceFail(true);

        try {
            KnowledgeOcrTaskVO vo = ocrService.createOcrTask(docIdA, "MINERU");
            Long taskId = vo.getId();

            // 轮询等待异步任务捕获异常并转为 FAILED
            boolean reached = awaitCondition(() -> {
                KnowledgeOcrTaskEntity t = taskMapper.selectById(taskId);
                return t != null && "FAILED".equals(t.getStatus());
            }, 5000, 100);

            Assertions.assertTrue(reached, "OCR 引擎异常时未在预期时间内转为 FAILED");

            KnowledgeOcrTaskVO failedVO = ocrService.getTaskStatus(taskId);
            Assertions.assertEquals("FAILED", failedVO.getStatus());
            Assertions.assertNotNull(failedVO.getErrorMsg());
            Assertions.assertTrue(failedVO.getErrorMsg().contains("Simulated OCR engine failure"));
        } finally {
            MockOcrEngineAdapter.setForceFail(false);
        }
    }
}
