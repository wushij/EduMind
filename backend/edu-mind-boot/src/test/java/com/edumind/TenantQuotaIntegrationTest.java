package com.edumind;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.TenantQuotaApi;
import com.edumind.system.dao.SysTenantQuotaDao;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.service.quota.TenantQuotaService;
import com.edumind.system.vo.tenant.TenantQuotaVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

/**
 * 智教云 V2.0 · 租户配额真实原子扣减与跨校隔离集成测试 (Gate I3)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantQuotaIntegrationTest {

    @Autowired
    private TenantQuotaService tenantQuotaService;

    @Autowired
    private TenantQuotaApi tenantQuotaApi;

    @Autowired
    private SysTenantQuotaDao sysTenantQuotaDao;

    @Autowired
    private com.edumind.system.dao.SysTenantDao sysTenantDao;

    private static final Long TENANT_A = 8801L;
    private static final Long TENANT_B = 8802L;

    @BeforeEach
    void setUp() {
        cleanAndInit();
    }

    @AfterEach
    void tearDown() {
        cleanAndInit();
    }

    private void cleanAndInit() {
        TenantContext.runWithoutTenant(() -> {
            ensureTenant(TENANT_A, "TENANT_8801", "测试学校8801");
            ensureTenant(TENANT_B, "TENANT_8802", "测试学校8802");
            initQuota(TENANT_A, 1000L, 0L, 80);
            initQuota(TENANT_B, 1000L, 0L, 80);
        });
        TenantContext.clear();
    }

    private void ensureTenant(Long tenantId, String code, String name) {
        com.edumind.system.entity.SysTenantEntity tenant = sysTenantDao.findById(tenantId);
        if (tenant == null) {
            tenant = new com.edumind.system.entity.SysTenantEntity();
            tenant.setId(tenantId);
            tenant.setCode(code);
            tenant.setName(name);
            tenant.setStatus(1);
            tenant.setPlanCode("PRO");
            tenant.setCreateTime(LocalDateTime.now());
            tenant.setUpdateTime(LocalDateTime.now());
            sysTenantDao.insert(tenant);
        }
    }

    private void initQuota(Long tenantId, Long limit, Long used, int warningThreshold) {
        TenantContext.runWithoutTenant(() -> {
            SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(tenantId, "TOKEN");
            if (entity != null) {
                entity.setLimitValue(limit);
                entity.setUsedValue(used);
                entity.setWarningThreshold(warningThreshold);
                entity.setUpdateTime(LocalDateTime.now());
                sysTenantQuotaDao.updateById(entity);
            } else {
                entity = new SysTenantQuotaEntity();
                entity.setTenantId(tenantId);
                entity.setQuotaType("TOKEN");
                entity.setLimitValue(limit);
                entity.setUsedValue(used);
                entity.setWarningThreshold(warningThreshold);
                entity.setUpdateTime(LocalDateTime.now());
                sysTenantQuotaDao.insert(entity);
            }
        });
    }

    @Test
    @DisplayName("测试租户 Token 配额原子扣减与超额 429 阻断")
    void testTokenQuotaAtomicConsumption() {
        TenantContext.setTenantId(TENANT_A);

        // 1. 首次扣减 600 Tokens (上限 1000) -> 成功
        boolean firstConsume = tenantQuotaService.consumeTokenQuota(TENANT_A, 600L);
        Assertions.assertTrue(firstConsume, "首次扣减应成功");

        TenantQuotaVO status1 = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertEquals(600L, status1.getUsedValue(), "已用配额应为 600");
        Assertions.assertEquals(60, status1.getUsagePercent(), "水位应为 60%");

        // 2. 二次扣减 500 Tokens (600+500=1100 > 1000) -> 超额，抛出 429 异常
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> {
            tenantQuotaService.consumeTokenQuota(TENANT_A, 500L);
        }, "超额扣减必须抛出异常");

        Assertions.assertEquals(ResultCode.TOO_MANY_REQUESTS.getCode(), ex.getCode(),
                "超额响应码必须为 429 (TOO_MANY_REQUESTS)");

        // 3. 验证事务原子性：扣减失败时，已消耗值绝不被脏写增加
        TenantQuotaVO status2 = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertEquals(600L, status2.getUsedValue(), "扣减失败后配额用量应保持 600 不变");

        // 4. 再次扣减 400 Tokens (恰好到达 1000 上限) -> 成功
        boolean thirdConsume = tenantQuotaService.consumeTokenQuota(TENANT_A, 400L);
        Assertions.assertTrue(thirdConsume);

        TenantQuotaVO status3 = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertEquals(1000L, status3.getUsedValue());
        Assertions.assertEquals(100, status3.getUsagePercent());

        // 5. 耗尽后进行预检阻断测试
        Assertions.assertThrows(BusinessException.class, () -> {
            tenantQuotaApi.checkTokenQuotaAvailable(TENANT_A);
        }, "配额耗尽后预检必须阻断");
    }

    @Test
    @DisplayName("测试租户 A 配额耗尽完全不影响租户 B (多租户配额严格隔离)")
    void testTenantQuotaIsolation() {
        // 1. 切换至 A 校，将 A 校配额全部耗尽
        TenantContext.setTenantId(TENANT_A);
        tenantQuotaService.consumeTokenQuota(TENANT_A, 1000L);
        Assertions.assertThrows(BusinessException.class, () -> {
            tenantQuotaService.consumeTokenQuota(TENANT_A, 1L);
        });

        // 2. 切换至 B 校，B 校配额应完全独立且可用
        TenantContext.setTenantId(TENANT_B);
        boolean consumeB = tenantQuotaService.consumeTokenQuota(TENANT_B, 500L);
        Assertions.assertTrue(consumeB, "B 校配额应正常扣减");

        TenantQuotaVO statusB = tenantQuotaService.getTokenQuotaStatus(TENANT_B);
        Assertions.assertEquals(500L, statusB.getUsedValue());
        Assertions.assertEquals(50, statusB.getUsagePercent());

        TenantContext.setTenantId(TENANT_A);
        TenantQuotaVO statusA = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertEquals(1000L, statusA.getUsedValue(), "A 校配额不受 B 校操作影响");
    }

    @Test
    @DisplayName("测试配额预警水位线 (warningThreshold) 计算与状态触发")
    void testQuotaWarningThreshold() {
        TenantContext.setTenantId(TENANT_A);

        // 阈值为 80%，消耗 750 -> 75% 未触发预警
        tenantQuotaService.consumeTokenQuota(TENANT_A, 750L);
        TenantQuotaVO status1 = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertFalse(status1.isWarning(), "未达到阈值时不应触发预警");

        // 追加消耗 100 -> 850 / 1000 = 85% >= 80% -> 触发预警
        tenantQuotaService.consumeTokenQuota(TENANT_A, 100L);
        TenantQuotaVO status2 = tenantQuotaService.getTokenQuotaStatus(TENANT_A);
        Assertions.assertTrue(status2.isWarning(), "达到预警水位线应触发 warning 标记");
    }
}
