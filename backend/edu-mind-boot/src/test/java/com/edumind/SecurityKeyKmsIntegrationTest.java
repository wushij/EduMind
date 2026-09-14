package com.edumind;

import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.service.memory.MemoryCryptoService;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.system.api.SecurityKeyQueryApi;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.service.security.SecurityKeyVersionService;
import com.edumind.system.vo.security.SecurityKeyVersionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 智教云 V2.0 · 国密 KMS 租户密钥版本管理与长期记忆接入集成测试 (Gate I9 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class SecurityKeyKmsIntegrationTest {

    private static final Long TENANT_A = 9950L;
    private static final Long TENANT_B = 9951L;
    private static final Long USER_ADMIN_A = 99501L;
    private static final Long USER_ADMIN_B = 99511L;
    private static final Long COURSE_ID_A = 8850L;
    private static final String DEFAULT_ALIAS = "edumind-data-key";

    @Autowired
    private SecurityKeyVersionService securityKeyVersionService;

    @Autowired
    private SecurityKeyQueryApi securityKeyQueryApi;

    @Autowired
    private MemoryCryptoService memoryCryptoService;

    @Autowired
    private AgentMemoryService agentMemoryService;

    @Autowired
    private AiMemoryDao aiMemoryDao;

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
                .id(USER_ADMIN_A)
                .username("kms_admin_a")
                .roles(List.of("TENANT_ADMIN"))
                .permissions(List.of("security:key:view", "security:key:rotate"))
                .build();
        UserContext.set(user);
    }

    private void setTenantAdminB() {
        TenantContext.setTenantId(TENANT_B);
        LoginUser user = LoginUser.builder()
                .id(USER_ADMIN_B)
                .username("kms_admin_b")
                .roles(List.of("TENANT_ADMIN"))
                .permissions(List.of("security:key:view", "security:key:rotate"))
                .build();
        UserContext.set(user);
    }

    private void cleanTestData() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM security_key_version WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM ai_memory_feedback WHERE memory_id IN (SELECT id FROM ai_memory_item WHERE namespace_id IN (SELECT id FROM ai_memory_namespace WHERE tenant_id IN (?, ?)))", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM ai_memory_item WHERE namespace_id IN (SELECT id FROM ai_memory_namespace WHERE tenant_id IN (?, ?))", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM ai_memory_namespace WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
    }

    @Test
    @DisplayName("Gate I9-1: 租户初次使用 KMS 自动懒初始化 v1 ACTIVE")
    public void testLazyInitAndDefaultActiveKey() {
        // 1. 调用 QueryApi 查询当前租户生效版本
        int activeVer = securityKeyQueryApi.getActiveKeyVersion(TENANT_A, DEFAULT_ALIAS);
        Assertions.assertEquals(1, activeVer, "初次使用应自动懒加载初始化 v1");

        // 2. 调用 Service 接口获取 active key VO
        SecurityKeyVersionVO activeVO = securityKeyVersionService.getActiveKey(DEFAULT_ALIAS);
        Assertions.assertNotNull(activeVO, "活跃密钥 VO 不能为空");
        Assertions.assertEquals(TENANT_A, activeVO.getTenantId());
        Assertions.assertEquals(DEFAULT_ALIAS, activeVO.getKeyAlias());
        Assertions.assertEquals(1, activeVO.getKeyVersion());
        Assertions.assertEquals("ACTIVE", activeVO.getStatus());
        Assertions.assertEquals("SM4-GCM", activeVO.getAlgorithm());

        // 3. 密钥列表应仅有 1 条 v1
        List<SecurityKeyVersionVO> list = securityKeyVersionService.listKeyVersions(DEFAULT_ALIAS);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(1, list.get(0).getKeyVersion());
    }

    @Test
    @DisplayName("Gate I9-2: 多租户密钥版本与轮换严格隔离")
    public void testTenantKeyIsolation() {
        // Tenant A 初始化并轮换至 v2
        setTenantAdminA();
        SecurityKeyRotateDTO rotateDTO = new SecurityKeyRotateDTO();
        rotateDTO.setKeyAlias(DEFAULT_ALIAS);
        SecurityKeyVersionVO rotatedA = securityKeyVersionService.rotateKey(rotateDTO);
        Assertions.assertEquals(2, rotatedA.getKeyVersion());
        Assertions.assertEquals("ACTIVE", rotatedA.getStatus());

        // 切换到 Tenant B
        setTenantAdminB();
        int activeVerB = securityKeyQueryApi.getActiveKeyVersion(TENANT_B, DEFAULT_ALIAS);
        Assertions.assertEquals(1, activeVerB, "Tenant B 初始版本应保持为 1，不受 Tenant A 轮换影响");

        List<SecurityKeyVersionVO> listB = securityKeyVersionService.listKeyVersions(DEFAULT_ALIAS);
        Assertions.assertEquals(1, listB.size(), "Tenant B 仅能看到自己的密钥版本");
        Assertions.assertEquals(TENANT_B, listB.get(0).getTenantId());

        // 再次切回 Tenant A，验证 Tenant A 具有两条记录 (v2 ACTIVE, v1 DEPRECATED)
        setTenantAdminA();
        List<SecurityKeyVersionVO> listA = securityKeyVersionService.listKeyVersions(DEFAULT_ALIAS);
        Assertions.assertEquals(2, listA.size());
        Assertions.assertEquals(2, listA.get(0).getKeyVersion());
        Assertions.assertEquals("ACTIVE", listA.get(0).getStatus());
        Assertions.assertEquals(1, listA.get(1).getKeyVersion());
        Assertions.assertEquals("DEPRECATED", listA.get(1).getStatus());
    }

    @Test
    @DisplayName("Gate I9-3: 密钥轮换生命周期状态机：旧 key DEPRECATED，新 key ACTIVE 且版本自增")
    public void testKeyRotationLifecycle() {
        setTenantAdminA();

        // 轮换第 1 次：v1 -> v2
        SecurityKeyRotateDTO rotate1 = new SecurityKeyRotateDTO();
        rotate1.setKeyAlias(DEFAULT_ALIAS);
        SecurityKeyVersionVO v2 = securityKeyVersionService.rotateKey(rotate1);
        Assertions.assertEquals(2, v2.getKeyVersion());
        Assertions.assertEquals("ACTIVE", v2.getStatus());

        // 轮换第 2 次：v2 -> v3
        SecurityKeyRotateDTO rotate2 = new SecurityKeyRotateDTO();
        rotate2.setKeyAlias(DEFAULT_ALIAS);
        SecurityKeyVersionVO v3 = securityKeyVersionService.rotateKey(rotate2);
        Assertions.assertEquals(3, v3.getKeyVersion());
        Assertions.assertEquals("ACTIVE", v3.getStatus());

        // 查询全量版本并验证状态机
        List<SecurityKeyVersionVO> all = securityKeyVersionService.listKeyVersions(DEFAULT_ALIAS);
        Assertions.assertEquals(3, all.size());
        Assertions.assertEquals(3, all.get(0).getKeyVersion());
        Assertions.assertEquals("ACTIVE", all.get(0).getStatus());

        Assertions.assertEquals(2, all.get(1).getKeyVersion());
        Assertions.assertEquals("DEPRECATED", all.get(1).getStatus());

        Assertions.assertEquals(1, all.get(2).getKeyVersion());
        Assertions.assertEquals("DEPRECATED", all.get(2).getStatus());
    }

    @Test
    @DisplayName("Gate I9-4: 密钥轮换后旧数据解密兼容性与 Fail-Closed 隐私保护")
    public void testDecryptLegacyDataAfterRotation() {
        setTenantAdminA();

        String rawSecretText = "EduMind_KMS_Confidential_Exam_2026";

        // 1. 在 v1 版本下加密敏感数据
        MemoryCryptoService.EncryptResult encV1 = memoryCryptoService.encryptWithVersion(TENANT_A, rawSecretText);
        Assertions.assertNotNull(encV1);
        Assertions.assertEquals(1, encV1.getKeyVersion(), "初次加密应使用 v1 密钥");
        String cipherV1 = encV1.getCiphertext();

        // 2. 执行密钥轮换：当前活跃版本变为 v2
        SecurityKeyRotateDTO rotateDTO = new SecurityKeyRotateDTO();
        rotateDTO.setKeyAlias(DEFAULT_ALIAS);
        securityKeyVersionService.rotateKey(rotateDTO);
        Assertions.assertEquals(2, securityKeyQueryApi.getActiveKeyVersion(TENANT_A, DEFAULT_ALIAS));

        // 3. 验证兼容性：旧数据携带 v1 版本解密，依然能够成功解密！
        String decryptedLegacy = memoryCryptoService.decrypt(TENANT_A, cipherV1, 1);
        Assertions.assertEquals(rawSecretText, decryptedLegacy, "轮换后旧数据必须支持通过旧版本正常解密");

        // 4. 验证 Fail-Closed：若错误使用 v2 密钥解密 v1 密文，必须静默返回 null，拒绝暴露数据
        String decryptedWithWrongKey = memoryCryptoService.decrypt(TENANT_A, cipherV1, 2);
        Assertions.assertNull(decryptedWithWrongKey, "Fail-Closed: 密钥版本不匹配时必须拒绝解密并返回 null");
    }

    @Test
    @DisplayName("Gate I9-5: 跨租户 IDOR 密钥访问越权拦截严格抛出 403")
    public void testIdorKeyAccessForbidden() {
        // Tenant A 初始化密钥
        setTenantAdminA();
        SecurityKeyVersionVO keyA = securityKeyVersionService.getActiveKey(DEFAULT_ALIAS);
        Long keyIdA = keyA.getId();
        Assertions.assertNotNull(keyIdA);

        // 切换为 Tenant B
        setTenantAdminB();

        // 1. Tenant B 企图直接查看 Tenant A 的密钥明细 -> 403
        BusinessException ex1 = Assertions.assertThrows(BusinessException.class, () -> {
            securityKeyVersionService.getKeyVersion(keyIdA);
        }, "租户 B 企图越权访问租户 A 密钥记录应被拦截");
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex1.getCode());

        // 2. Tenant B 企图传入 keyIdA 触发轮换 -> 403
        SecurityKeyRotateDTO illegalRotate = new SecurityKeyRotateDTO();
        illegalRotate.setId(keyIdA);
        illegalRotate.setKeyAlias(DEFAULT_ALIAS);
        BusinessException ex2 = Assertions.assertThrows(BusinessException.class, () -> {
            securityKeyVersionService.rotateKey(illegalRotate);
        }, "租户 B 企图越权轮换租户 A 密钥记录应被拦截");
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("Gate I9-6: AgentMemory 全链路协同：记录 KMS key_version 且检索解密成功")
    public void testAgentMemoryIntegrationWithKeyVersion() {
        setTenantAdminA();

        // 1. 用户授权长期记忆知情同意
        MemoryConsentDTO consent = new MemoryConsentDTO();
        consent.setCourseId(COURSE_ID_A);
        consent.setConsent(true);
        consent.setRetentionDays(30);
        agentMemoryService.updateConsent(consent);

        // 2. 轮换 KMS 密钥至 v2
        SecurityKeyRotateDTO rotateDTO = new SecurityKeyRotateDTO();
        rotateDTO.setKeyAlias(DEFAULT_ALIAS);
        securityKeyVersionService.rotateKey(rotateDTO);
        Assertions.assertEquals(2, securityKeyQueryApi.getActiveKeyVersion(TENANT_A, DEFAULT_ALIAS));

        // 3. 写入高敏记忆条目
        String sensitiveContent = "学生在国密算法章节弱项：SM4 分组模式 CBC/GCM 理解薄弱";
        MemoryItemCreateDTO itemDTO = new MemoryItemCreateDTO();
        itemDTO.setCourseId(COURSE_ID_A);
        itemDTO.setMemoryType("FACT");
        itemDTO.setSummary("国密算法学习弱项");
        itemDTO.setFullContent(sensitiveContent);
        itemDTO.setSensitivityLevel("HIGH_RISK");

        Long memoryId = agentMemoryService.createMemoryItem(itemDTO);
        Assertions.assertNotNull(memoryId);

        // 4. 验证数据库落盘实体中的 key_version 是否为 2
        AiMemoryItemEntity itemEntity = aiMemoryDao.findItemById(memoryId);
        Assertions.assertNotNull(itemEntity);
        Assertions.assertEquals(2, itemEntity.getKeyVersion(), "新写入的加密条目 key_version 必须与当时生效 KMS 版本匹配");
        Assertions.assertNotNull(itemEntity.getContentCiphertext());

        // 5. 执行长期记忆检索召回，验证解密后的 fullContent 正确还原
        List<MemoryItemVO> retrieved = agentMemoryService.retrieveMemories(COURSE_ID_A, "国密算法");
        Assertions.assertFalse(retrieved.isEmpty(), "应成功召回国密记忆");
        MemoryItemVO matched = retrieved.stream().filter(m -> m.getId().equals(memoryId)).findFirst().orElse(null);
        Assertions.assertNotNull(matched);
        Assertions.assertEquals(sensitiveContent, matched.getFullContent(), "检索结果应成功利用 v2 密钥解密敏感文本");
    }
}
