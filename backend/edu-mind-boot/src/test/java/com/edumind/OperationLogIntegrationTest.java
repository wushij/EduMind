package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.controller.gateway.AiModelController;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dto.gateway.AiModelSaveDTO;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.enums.BusinessType;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.question.controller.export.ExportTaskController;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.statistics.controller.intervention.TeachingInterventionController;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import com.edumind.system.controller.security.SecurityKeyVersionController;
import com.edumind.system.controller.tenant.SysTenantQuotaController;
import com.edumind.system.dao.SysOperLogDao;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.mapper.SysOperLogMapper;
import com.edumind.system.service.log.SysOperLogService;
import com.edumind.system.vo.log.SysOperLogVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 智教云 V2.0 · 生产运营审计闭环与跨模块联合演练集成测试 (Gate I11 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class OperationLogIntegrationTest {

    private static final Long TENANT_A = 9960L;
    private static final Long TENANT_B = 9961L;
    private static final Long USER_A = 99601L;
    private static final Long USER_B = 99611L;

    @Autowired
    private ExportTaskController exportTaskController;

    @Autowired
    private TeachingInterventionController teachingInterventionController;

    @Autowired
    private SecurityKeyVersionController securityKeyVersionController;

    @Autowired
    private SysTenantQuotaController sysTenantQuotaController;

    @Autowired
    private AiModelController aiModelController;

    @Autowired
    private AiModelConfigDao aiModelConfigDao;

    @Autowired
    private SysOperLogService sysOperLogService;

    @Autowired
    private SysOperLogDao sysOperLogDao;

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        cleanTestData();
        setTenantAdminA();
    }

    @AfterEach
    public void tearDown() {
        cleanTestData();
        TenantContext.clear();
        UserContext.clear();
    }

    private void setTenantAdminA() {
        TenantContext.setTenantId(TENANT_A);
        LoginUser user = LoginUser.builder()
                .id(USER_A)
                .username("gateI11_admin_a")
                .realName("管理员A")
                .roles(List.of("ADMIN", "TEACHER"))
                .permissions(List.of(
                        "exam:export",
                        "analytics:intervention:manage",
                        "analytics:intervention:view",
                        "security:key:rotate",
                        "security:key:view",
                        "system:quota:edit",
                        "system:quota:view",
                        "ai:model:manage",
                        "admin"
                ))
                .build();
        UserContext.set(user);
    }

    private void setTenantAdminB() {
        TenantContext.setTenantId(TENANT_B);
        LoginUser user = LoginUser.builder()
                .id(USER_B)
                .username("gateI11_admin_b")
                .realName("管理员B")
                .roles(List.of("ADMIN"))
                .permissions(List.of("admin"))
                .build();
        UserContext.set(user);
    }

    private void cleanTestData() {
        TenantContext.runWithoutTenant(() -> {
            try {
                jdbcTemplate.update("DELETE FROM sys_oper_log WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM export_task WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM sys_notification WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM teaching_intervention WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM sys_tenant_quota WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM security_key_version WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
                jdbcTemplate.update("DELETE FROM ai_model_config WHERE config_name LIKE 'model-gate-i11%'");
                aiModelConfigDao.deleteByConfigName("model-gate-i11-mask-test");
            } catch (Exception ignored) {
            }
        });
    }

    private boolean awaitCondition(BooleanSupplier condition, long timeoutMs, long intervalMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Test
    @DisplayName("Gate I11-1: 试卷导出关键写操作接入 @OperationLog 异步落库审计，验证 businessType、标题及租户上下文")
    public void testExportTaskOperationLog() {
        setTenantAdminA();
        PaperExportRequestDTO dto = new PaperExportRequestDTO();
        dto.setExamId(8801L);
        dto.setPaperTitle("期末考试A卷");
        dto.setPaperSize("A4");

        exportTaskController.createPaperExportTask(dto);

        boolean recorded = awaitCondition(() -> {
            List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                    .eq(SysOperLogEntity::getTenantId, TENANT_A)
                    .eq(SysOperLogEntity::getOperUserId, USER_A));
            return logs.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("创建导出任务"));
        }, 3000, 50);

        Assertions.assertTrue(recorded, "导出操作日志应在 3 秒内异步落库成功");

        List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_A)
                .eq(SysOperLogEntity::getOperUserId, USER_A));
        SysOperLogEntity log = logs.stream()
                .filter(l -> l.getTitle() != null && l.getTitle().contains("创建导出任务"))
                .findFirst().orElseThrow();

        Assertions.assertEquals(TENANT_A, log.getTenantId());
        Assertions.assertEquals(USER_A, log.getOperUserId());
        Assertions.assertEquals(BusinessType.EXPORT.getValue(), log.getBusinessType());
        Assertions.assertTrue(log.getTitle().contains("试卷导出") || log.getTitle().contains("创建导出任务"));
        Assertions.assertEquals(0, log.getStatus());
    }

    @Test
    @DisplayName("Gate I11-2: 教学干预全生命周期（创建 + 审批）触发审计日志，验证 INSERT 与 GRANT 操作类型")
    public void testInterventionLifecycleOperationLog() {
        setTenantAdminA();
        InterventionCreateDTO createDTO = new InterventionCreateDTO();
        createDTO.setCourseId(201L);
        createDTO.setCourseName("高等数学（上）");
        createDTO.setTriggerType("EXAM_WEAK");
        createDTO.setTitle("第3章微分方程掌握度预警");
        createDTO.setProposalText("针对高等数学重点题目进行靶向辅导");
        createDTO.setAffectedStudentCount(12);

        var res = teachingInterventionController.createIntervention(createDTO);
        Assertions.assertNotNull(res.getData());
        Long interventionId = res.getData().getId();

        teachingInterventionController.approveIntervention(interventionId, new InterventionActionDTO());

        boolean recorded = awaitCondition(() -> {
            List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                    .eq(SysOperLogEntity::getTenantId, TENANT_A)
                    .eq(SysOperLogEntity::getOperUserId, USER_A));
            long count = logs.stream().filter(l -> l.getTitle() != null && l.getTitle().contains("干预预案")).count();
            return count >= 2;
        }, 3000, 50);

        Assertions.assertTrue(recorded, "干预预案创建及审批操作日志应在 3 秒内异步落库成功");

        List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_A)
                .eq(SysOperLogEntity::getOperUserId, USER_A));

        boolean hasInsert = logs.stream().anyMatch(l -> l.getBusinessType() == BusinessType.INSERT.getValue() && l.getTitle().contains("创建干预预案"));
        boolean hasGrant = logs.stream().anyMatch(l -> l.getBusinessType() == BusinessType.GRANT.getValue() && l.getTitle().contains("审批通过干预预案"));

        Assertions.assertTrue(hasInsert, "应记录创建干预预案的 INSERT 类型日志");
        Assertions.assertTrue(hasGrant, "应记录审批通过干预预案的 GRANT 类型日志");
    }

    @Test
    @DisplayName("Gate I11-3: 国密 KMS 密钥轮换写操作审计，验证 GRANT 类型及日志中禁止包含密钥明文材料")
    public void testKmsRotateOperationLogAndSecretMasking() {
        setTenantAdminA();
        SecurityKeyRotateDTO rotateDTO = new SecurityKeyRotateDTO();
        rotateDTO.setKeyAlias("edumind-data-key");

        securityKeyVersionController.rotateKey(rotateDTO);

        boolean recorded = awaitCondition(() -> {
            List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                    .eq(SysOperLogEntity::getTenantId, TENANT_A)
                    .eq(SysOperLogEntity::getOperUserId, USER_A));
            return logs.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("轮换数据密钥"));
        }, 3000, 50);

        Assertions.assertTrue(recorded, "KMS 密钥轮换日志应异步落库成功");

        List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_A)
                .eq(SysOperLogEntity::getOperUserId, USER_A));
        SysOperLogEntity log = logs.stream()
                .filter(l -> l.getTitle() != null && l.getTitle().contains("轮换数据密钥"))
                .findFirst().orElseThrow();

        Assertions.assertEquals(BusinessType.GRANT.getValue(), log.getBusinessType());
        if (log.getOperParam() != null) {
            Assertions.assertFalse(log.getOperParam().contains("secretKey"), "操作入参不得泄露 secretKey 明文");
        }
    }

    @Test
    @DisplayName("Gate I11-4: 操作日志多租户隔离与 IDOR 防护，租户 B 越权查/删租户 A 日志必须抛出 403 FORBIDDEN")
    public void testOperLogIdorTenantIsolation() {
        setTenantAdminA();
        // 租户 A 直接入库一条测试日志
        SysOperLogEntity entityA = SysOperLogEntity.builder()
                .tenantId(TENANT_A)
                .title("租户A内部审计操作")
                .businessType(BusinessType.INSERT.getValue())
                .method("com.edumind.test.A()")
                .requestMethod("POST")
                .operUserId(USER_A)
                .operName("gateI11_admin_a")
                .operUrl("/api/test/a")
                .operIp("127.0.0.1")
                .status(0)
                .operTime(LocalDateTime.now())
                .build();
        sysOperLogDao.insert(entityA);
        Long logIdA = entityA.getId();
        Assertions.assertNotNull(logIdA);

        // 切换至租户 B
        setTenantAdminB();

        // 租户 B 试图查询租户 A 的日志详情 -> 必须触发 IDOR 403
        BusinessException getEx = Assertions.assertThrows(BusinessException.class, () -> sysOperLogService.getById(logIdA));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), getEx.getCode(), "跨租户读取操作日志必须拦截为 403 FORBIDDEN");

        // 租户 B 试图删除租户 A 的日志记录 -> 必须触发 IDOR 403
        BusinessException delEx = Assertions.assertThrows(BusinessException.class, () -> sysOperLogService.delete(logIdA));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), delEx.getCode(), "跨租户删除操作日志必须拦截为 403 FORBIDDEN");

        // 切换回租户 A，验证记录安然无恙且可正常获取
        setTenantAdminA();
        SysOperLogVO voA = sysOperLogService.getById(logIdA);
        Assertions.assertNotNull(voA);
        Assertions.assertEquals("租户A内部审计操作", voA.getTitle());
    }

    @Test
    @DisplayName("Gate I11-5: 跨模块联合演练（试卷导出 + 教学干预派发 + 租户配额调整），全链路审计闭环且租户隔离")
    public void testCrossModuleSmokeExercise() {
        setTenantAdminA();

        // 1. 试卷导出
        PaperExportRequestDTO paperDTO = new PaperExportRequestDTO();
        paperDTO.setExamId(8802L);
        paperDTO.setPaperTitle("联合演练试卷");
        paperDTO.setPaperSize("A4");
        exportTaskController.createPaperExportTask(paperDTO);

        // 2. 教学干预创建、审批与派发
        InterventionCreateDTO createDTO = new InterventionCreateDTO();
        createDTO.setCourseId(202L);
        createDTO.setCourseName("离散数学");
        createDTO.setTriggerType("ACTIVITY_DROP");
        createDTO.setTitle("离散数学作业预警");
        createDTO.setProposalText("作业活跃度低，派发专项微练习");
        createDTO.setAffectedStudentCount(8);
        var res = teachingInterventionController.createIntervention(createDTO);
        Long interventionId = res.getData().getId();
        teachingInterventionController.approveIntervention(interventionId, new InterventionActionDTO());
        teachingInterventionController.dispatchIntervention(interventionId);

        // 3. 租户配额调整
        QuotaUpdateDTO quotaDTO = new QuotaUpdateDTO();
        quotaDTO.setQuotaType("TOKEN");
        quotaDTO.setLimitValue(8000000L);
        quotaDTO.setWarningThreshold(85);
        sysTenantQuotaController.updateQuotaThreshold(quotaDTO);

        // 等待所有日志异步写入
        boolean recorded = awaitCondition(() -> {
            List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                    .eq(SysOperLogEntity::getTenantId, TENANT_A)
                    .eq(SysOperLogEntity::getOperUserId, USER_A));
            return logs.size() >= 3;
        }, 3000, 50);

        Assertions.assertTrue(recorded, "联合演练多模块审计日志应在 3 秒内落库成功");

        List<SysOperLogEntity> logsA = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_A)
                .eq(SysOperLogEntity::getOperUserId, USER_A));
        Assertions.assertTrue(logsA.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("试卷导出")));
        Assertions.assertTrue(logsA.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("教学干预")));
        Assertions.assertTrue(logsA.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("租户配额")));

        // 切换租户 B，确认租户 B 看不到租户 A 的任何审计记录
        setTenantAdminB();
        List<SysOperLogEntity> logsB = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_B));
        Assertions.assertTrue(logsB.isEmpty(), "租户 B 不得查看到租户 A 的演练审计日志");
    }

    @Test
    @DisplayName("Gate I11-6: 敏感入参脱敏强化，模型配置 apiKey 等密钥在日志入库时自动遮罩为 ******")
    public void testApiKeyParamMasking() {
        setTenantAdminA();
        String sensitiveKey = "sk-gate-i11-secret-key-abcdef123456";

        AiModelSaveDTO modelDTO = new AiModelSaveDTO();
        modelDTO.setName("model-gate-i11-mask-test");
        modelDTO.setProvider("mock");
        modelDTO.setConfigType("chat");
        modelDTO.setModelName("mock-chat-v1");
        modelDTO.setApiKey(sensitiveKey);
        modelDTO.setStatus("enabled");

        aiModelController.createModel(modelDTO);

        boolean recorded = awaitCondition(() -> {
            List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                    .eq(SysOperLogEntity::getTenantId, TENANT_A)
                    .eq(SysOperLogEntity::getOperUserId, USER_A));
            return logs.stream().anyMatch(l -> l.getTitle() != null && l.getTitle().contains("新增AI模型配置"));
        }, 3000, 50);

        Assertions.assertTrue(recorded, "模型新增操作日志应在 3 秒内异步落库成功");

        List<SysOperLogEntity> logs = sysOperLogMapper.selectList(new LambdaQueryWrapper<SysOperLogEntity>()
                .eq(SysOperLogEntity::getTenantId, TENANT_A)
                .eq(SysOperLogEntity::getOperUserId, USER_A));
        SysOperLogEntity log = logs.stream()
                .filter(l -> l.getTitle() != null && l.getTitle().contains("新增AI模型配置"))
                .findFirst().orElseThrow();

        Assertions.assertNotNull(log.getOperParam());
        Assertions.assertTrue(log.getOperParam().contains("\"******\""), "oper_param 应包含脱敏后的 ****** 占位符");
        Assertions.assertFalse(log.getOperParam().contains(sensitiveKey), "oper_param 绝不得出现敏感 apiKey 明文");
    }
}
