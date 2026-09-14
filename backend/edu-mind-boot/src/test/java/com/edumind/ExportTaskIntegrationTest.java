package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.question.dao.export.ExportTaskDao;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.entity.export.ExportTaskEntity;
import com.edumind.question.integration.export.impl.MockPaperExportEngine;
import com.edumind.question.mapper.export.ExportTaskMapper;
import com.edumind.question.service.export.ExportTaskService;
import com.edumind.question.vo.export.ExportTaskVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 智教云 V2.0 · 试卷导出 Worker 异步化与鉴权全链路集成测试 (Gate I7 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class ExportTaskIntegrationTest {

    private static final Long TENANT_A = 9930L;
    private static final Long TENANT_B = 9931L;
    private static final Long USER_TEACHER_A = 99301L;
    private static final Long USER_TEACHER_B = 99311L;
    private static final Long EXAM_ID = 8801L;

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private ExportTaskDao exportTaskDao;

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${minio.bucketName:edumind}")
    private String bucketName;

    @BeforeEach
    public void setUp() {
        MockPaperExportEngine.setForceFail(false);
        MockPaperExportEngine.setMockDelayMs(50); // 加速集成测试

        cleanTestData();

        // 默认初始化为 Tenant A 教师用户上下文
        TenantContext.setTenantId(TENANT_A);
        LoginUser user = LoginUser.builder()
                .id(USER_TEACHER_A)
                .username("gateI7_teacher_a")
                .roles(List.of(SecurityConstant.ROLE_ADMIN))
                .permissions(List.of("exam:export"))
                .build();
        UserContext.set(user);
    }

    @AfterEach
    public void tearDown() {
        MockPaperExportEngine.setForceFail(false);
        MockPaperExportEngine.setMockDelayMs(200);
        cleanTestData();
        TenantContext.clear();
        UserContext.clear();
    }

    private void cleanTestData() {
        TenantContext.clear();
        // 清理 Tenant A 与 Tenant B 的测试导出任务及存储文件
        List<ExportTaskEntity> tasks = exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTaskEntity>()
                .in(ExportTaskEntity::getTenantId, List.of(TENANT_A, TENANT_B)));
        for (ExportTaskEntity task : tasks) {
            if (task.getObjectKey() != null && !task.getObjectKey().isBlank()) {
                try {
                    fileStorageService.deleteFile(bucketName, task.getObjectKey());
                } catch (Exception ignored) {
                }
            }
            exportTaskMapper.deleteById(task.getId());
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

    private PaperExportRequestDTO buildSampleRequest(Long examId) {
        PaperExportRequestDTO dto = new PaperExportRequestDTO();
        dto.setExamId(examId != null ? examId : EXAM_ID);
        dto.setPaperTitle("2026年Gate I7高三开学数学综合模考卷");
        dto.setPaperSubtitle("理科数学 (全卷共4页 满分150分)");
        dto.setPaperSize("A4");
        dto.setShowWatermark(true);
        dto.setWatermarkText("智教云 · Gate I7 内部测试");
        dto.setShowAnswerSheet(true);
        dto.setShowAnalysis(true);
        dto.setShowStudentInfo(true);
        dto.setShowScoreGrid(true);
        return dto;
    }

    @Test
    @DisplayName("用例1: 创建导出任务立即返回 PENDING 初始态，进度为0且无下载链接")
    public void testCreatePaperExportTask_ReturnsPendingWithoutDownloadUrl() {
        TenantContext.setTenantId(TENANT_A);

        PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
        ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);

        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getTaskId());
        Assertions.assertEquals(TENANT_A, vo.getTenantId());
        Assertions.assertEquals(USER_TEACHER_A, vo.getUserId());
        Assertions.assertEquals("PENDING", vo.getStatus());
        Assertions.assertEquals(0, vo.getProgress());
        Assertions.assertNull(vo.getDownloadUrl(), "创建导出任务时严禁同步生成有效下载链接 (拒绝假完成)");

        // 验证数据库记录已存在且关联 EXAM_ID
        ExportTaskEntity entity = exportTaskDao.findById(Long.valueOf(vo.getTaskId()));
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(EXAM_ID, entity.getBizId());
        Assertions.assertEquals("EXAM_PAPER", entity.getBizType());
        Assertions.assertNotNull(entity.getExportParams());
    }

    @Test
    @DisplayName("用例2: 异步 Dispatcher 派发后状态机成功流转至 SUCCESS，生成 ObjectKey 与 DownloadToken")
    public void testAsyncExecution_FlowsToSuccess() {
        TenantContext.setTenantId(TENANT_A);

        PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
        ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);
        Long taskId = Long.valueOf(vo.getTaskId());

        // 异步等待流转为 SUCCESS (最多 5 秒)
        boolean completed = awaitCondition(() -> {
            ExportTaskVO current = exportTaskService.getTaskStatus(taskId);
            return "SUCCESS".equalsIgnoreCase(current.getStatus());
        }, 5000, 50);

        Assertions.assertTrue(completed, "异步任务应在时限内执行完成并进入 SUCCESS 状态");

        ExportTaskVO successVo = exportTaskService.getTaskStatus(taskId);
        Assertions.assertEquals("SUCCESS", successVo.getStatus());
        Assertions.assertEquals(100, successVo.getProgress());
        Assertions.assertNotNull(successVo.getDownloadUrl());
        Assertions.assertTrue(successVo.getDownloadUrl().contains("/api/question/exports/" + taskId + "/download?token="));

        // 校验底层持久化字段
        ExportTaskEntity entity = exportTaskDao.findById(taskId);
        Assertions.assertNotNull(entity.getDownloadToken());
        Assertions.assertNotNull(entity.getObjectKey());
        Assertions.assertTrue(entity.getObjectKey().contains("tenants/" + TENANT_A + "/export/" + taskId + "/"));
    }

    @Test
    @DisplayName("用例3: 带有效 Token 下载试卷成功，响应流包含试卷与考务排版特征数据")
    public void testDownload_SuccessWithValidToken() throws Exception {
        TenantContext.setTenantId(TENANT_A);

        PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
        ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);
        Long taskId = Long.valueOf(vo.getTaskId());

        boolean completed = awaitCondition(() -> {
            ExportTaskEntity entity = exportTaskDao.findById(taskId);
            return entity != null && "SUCCESS".equalsIgnoreCase(entity.getStatus());
        }, 5000, 50);
        Assertions.assertTrue(completed);

        ExportTaskEntity entity = exportTaskDao.findById(taskId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        exportTaskService.download(taskId, entity.getDownloadToken(), response);

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("application/pdf", response.getContentType());
        Assertions.assertTrue(response.getHeader("Content-Disposition").contains("attachment;"));

        byte[] content = response.getContentAsByteArray();
        Assertions.assertTrue(content.length > 0);
        String text = new String(content, StandardCharsets.UTF_8);
        Assertions.assertTrue(text.contains("TaskId: " + taskId));
        Assertions.assertTrue(text.contains("TenantId: " + TENANT_A));
        Assertions.assertTrue(text.contains("ExamId: " + EXAM_ID));
        Assertions.assertTrue(text.contains("Gate I7"));
    }

    @Test
    @DisplayName("用例4: 非法/伪造 Token 请求下载，触发鉴权拦截并抛出 403")
    public void testDownload_InvalidToken_ThrowsForbidden() {
        TenantContext.setTenantId(TENANT_A);

        PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
        ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);
        Long taskId = Long.valueOf(vo.getTaskId());

        boolean completed = awaitCondition(() -> {
            ExportTaskEntity entity = exportTaskDao.findById(taskId);
            return entity != null && "SUCCESS".equalsIgnoreCase(entity.getStatus());
        }, 5000, 50);
        Assertions.assertTrue(completed);

        MockHttpServletResponse response = new MockHttpServletResponse();
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                exportTaskService.download(taskId, "forged_token_123456", response));

        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode(), "非法Token下载必须返回 403 禁止访问");
    }

    @Test
    @DisplayName("用例5: 跨租户越权访问 (IDOR) 拦截，Tenant B 试图获取或下载 Tenant A 导出任务抛出 403")
    public void testCrossTenant_IDOR_Blocked() {
        // 1. 在 Tenant A 下创建并完成导出任务
        TenantContext.setTenantId(TENANT_A);
        PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
        ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);
        Long taskId = Long.valueOf(vo.getTaskId());

        boolean completed = awaitCondition(() -> {
            ExportTaskEntity entity = exportTaskDao.findById(taskId);
            return entity != null && "SUCCESS".equalsIgnoreCase(entity.getStatus());
        }, 5000, 50);
        Assertions.assertTrue(completed);
        ExportTaskEntity entity = exportTaskDao.findById(taskId);

        // 2. 切换为 Tenant B 教师上下文
        TenantContext.setTenantId(TENANT_B);
        LoginUser userB = LoginUser.builder()
                .id(USER_TEACHER_B)
                .username("gateI7_teacher_b")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .permissions(List.of("exam:export"))
                .build();
        UserContext.set(userB);

        // 3. 验证 Tenant B 查询 Tenant A 任务状态抛出 403
        BusinessException getStatusEx = Assertions.assertThrows(BusinessException.class, () ->
                exportTaskService.getTaskStatus(taskId));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), getStatusEx.getCode(), "跨租户查询任务状态必须被 IDOR 拦截 (403)");

        // 4. 验证 Tenant B 即便猜到 Token 下载 Tenant A 文件仍抛出 403
        MockHttpServletResponse response = new MockHttpServletResponse();
        BusinessException downloadEx = Assertions.assertThrows(BusinessException.class, () ->
                exportTaskService.download(taskId, entity.getDownloadToken(), response));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), downloadEx.getCode(), "跨租户下载必须被 IDOR 拦截 (403)");
    }

    @Test
    @DisplayName("用例6: Mock 引擎强行失败注入，任务状态机正确流转至 FAILED 并持久化失败原因")
    public void testAsyncExecution_ForceFail_FlowsToFailed() {
        TenantContext.setTenantId(TENANT_A);
        MockPaperExportEngine.setForceFail(true);

        try {
            PaperExportRequestDTO dto = buildSampleRequest(EXAM_ID);
            ExportTaskVO vo = exportTaskService.createPaperExportTask(dto);
            Long taskId = Long.valueOf(vo.getTaskId());

            // 异步等待流转为 FAILED (最多 5 秒)
            boolean failed = awaitCondition(() -> {
                ExportTaskEntity entity = exportTaskDao.findById(taskId);
                return entity != null && "FAILED".equalsIgnoreCase(entity.getStatus());
            }, 5000, 50);

            Assertions.assertTrue(failed, "强行失败注入后任务应进入 FAILED 状态");

            ExportTaskVO failedVo = exportTaskService.getTaskStatus(taskId);
            Assertions.assertEquals("FAILED", failedVo.getStatus());
            Assertions.assertEquals(0, failedVo.getProgress());
            Assertions.assertNotNull(failedVo.getErrorMsg());
            Assertions.assertTrue(failedVo.getErrorMsg().contains("forced failure"));
        } finally {
            MockPaperExportEngine.setForceFail(false);
        }
    }
}
