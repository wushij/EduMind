package com.edumind;

import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dto.gateway.AiModelSaveDTO;
import com.edumind.ai.dto.gateway.AiModelTestDTO;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.integration.crypto.AiApiKeyCipherService;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.service.gateway.AiModelManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.AiModelTestResultVO;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.security.crypto.Sm4Service;
import com.edumind.system.api.SecurityKeyQueryApi;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.service.security.SecurityKeyVersionService;
import com.edumind.system.vo.security.SecurityKeyVersionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 智教云 V2.0 · AI 模型 API Key 接 KMS 密钥版本追踪与平滑解密集成测试 (Gate I10 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class AiModelKeyKmsIntegrationTest {

    private static final String TEST_MODEL_A = "model-gate-i10-test-a";
    private static final String TEST_MODEL_B = "model-gate-i10-test-b";
    private static final String MODEL_KEY_ALIAS = "edumind-model-key";
    private static final Long PLATFORM_TENANT = 1L;
    private static final Long ADMIN_USER_ID = 10001L;

    @Autowired
    private AiModelManageService aiModelManageService;

    @Autowired
    private AiModelConfigDao aiModelConfigDao;

    @Autowired
    private AiApiKeyCipherService aiApiKeyCipherService;

    @Autowired
    private SecurityKeyVersionService securityKeyVersionService;

    @Autowired
    private SecurityKeyQueryApi securityKeyQueryApi;

    @Autowired
    private LlmClientRegistry llmClientRegistry;

    @Autowired
    private Sm4Service sm4Service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${edumind.ai.model-key-secret:EduMind_AI_Model_KEK}")
    private String modelKeySecret;

    @BeforeEach
    public void setUp() {
        cleanTestData();
        setPlatformAdminContext();
        resetKmsModelKeyToV1();
    }

    @AfterEach
    public void tearDown() {
        cleanTestData();
        resetKmsModelKeyToV1();
        TenantContext.clear();
        UserContext.clear();
    }

    private void setPlatformAdminContext() {
        TenantContext.setTenantId(PLATFORM_TENANT);
        LoginUser user = LoginUser.builder()
                .id(ADMIN_USER_ID)
                .username("platform_admin")
                .roles(List.of("PLATFORM_ADMIN", "TENANT_ADMIN"))
                .permissions(List.of("security:key:view", "security:key:rotate", "ai:model:manage"))
                .build();
        UserContext.set(user);
    }

    private void cleanTestData() {
        aiModelConfigDao.deleteByConfigName(TEST_MODEL_A);
        aiModelConfigDao.deleteByConfigName(TEST_MODEL_B);
        llmClientRegistry.invalidateAll();
    }

    private void resetKmsModelKeyToV1() {
        try {
            // 清理大于版本 1 的轮换测试数据，并保证版本 1 处于 ACTIVE 状态
            jdbcTemplate.update("DELETE FROM security_key_version WHERE tenant_id = ? AND key_alias = ? AND key_version > 1",
                    PLATFORM_TENANT, MODEL_KEY_ALIAS);
            int updated = jdbcTemplate.update("UPDATE security_key_version SET status = 'ACTIVE' WHERE tenant_id = ? AND key_alias = ? AND key_version = 1",
                    PLATFORM_TENANT, MODEL_KEY_ALIAS);
            if (updated == 0) {
                // 如果数据库尚未初始化种子数据，插入版本 1
                jdbcTemplate.update("INSERT INTO security_key_version (tenant_id, key_alias, key_version, algorithm, status, activated_time, create_time, update_time) " +
                                "VALUES (?, ?, 1, 'SM4_GCM', 'ACTIVE', NOW(), NOW(), NOW())",
                        PLATFORM_TENANT, MODEL_KEY_ALIAS);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Gate I10-1: 保存模型时 API Key 国密加密存储，禁止明文落盘，并追踪 key_version")
    public void testSaveModelApiKeyEncrypted() {
        String rawApiKey = "sk-gate-i10-secret-api-key-99887766";
        AiModelSaveDTO dto = new AiModelSaveDTO();
        dto.setName(TEST_MODEL_A);
        dto.setProvider("mock");
        dto.setConfigType("chat");
        dto.setModelName("mock-model-v1");
        dto.setApiKey(rawApiKey);
        dto.setStatus("enabled");

        AiModelConfigVO vo = aiModelManageService.createModel(dto);
        Assertions.assertNotNull(vo);

        // 绕过服务层，直接从数据库底层校验落盘实体
        AiModelConfigEntity entity = aiModelConfigDao.findByConfigName(TEST_MODEL_A);
        Assertions.assertNotNull(entity, "数据库必须能查到持久化实体");
        Assertions.assertNotNull(entity.getApiKeyCipher(), "apiKeyCipher 必须非空");
        Assertions.assertNotEquals(rawApiKey, entity.getApiKeyCipher(), "必须为密文，严禁明文落盘");
        Assertions.assertFalse(entity.getApiKeyCipher().startsWith("sk-"), "密文严禁以明文 sk- 开头");
        Assertions.assertNotNull(entity.getKeyVersion(), "key_version 必须记录当前有效密钥版本");
        Assertions.assertEquals(1, entity.getKeyVersion(), "初次保存默认绑定 KMS 种子版本 v1");
    }

    @Test
    @DisplayName("Gate I10-2: 模型配置 VO 严格脱敏，不回传 apiKey 明文，仅暴露 hasApiKey 与 keyVersion")
    public void testListVoDoesNotExposeApiKey() {
        String rawApiKey = "sk-gate-i10-secret-key-for-vo-test";
        AiModelSaveDTO dto = new AiModelSaveDTO();
        dto.setName(TEST_MODEL_A);
        dto.setProvider("mock");
        dto.setConfigType("chat");
        dto.setModelName("mock-model-v1");
        dto.setApiKey(rawApiKey);
        dto.setStatus("enabled");
        aiModelManageService.createModel(dto);

        List<AiModelConfigVO> list = aiModelManageService.listModels("chat", "enabled");
        AiModelConfigVO target = list.stream()
                .filter(m -> TEST_MODEL_A.equals(m.getName()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(target, "列表必须包含测试模型配置");
        Assertions.assertTrue(Boolean.TRUE.equals(target.getHasApiKey()), "hasApiKey 标志必须为 true");
        Assertions.assertNull(target.getApiKey(), "VO 返回时 apiKey 必须强制为 null，严禁向前端泄漏秘钥明文");
        Assertions.assertNotNull(target.getKeyVersion(), "VO 必须包含 keyVersion 元数据用于安全版本展示");
        Assertions.assertEquals(1, target.getKeyVersion());
    }

    @Test
    @DisplayName("Gate I10-3: 连通性测试与按版本解密闭环验证")
    public void testDecryptForConnectivityTest() {
        String rawApiKey = "sk-gate-i10-connectivity-token-xyz";
        AiModelSaveDTO dto = new AiModelSaveDTO();
        dto.setName(TEST_MODEL_A);
        dto.setProvider("mock");
        dto.setConfigType("chat");
        dto.setModelName("mock-model-v1");
        dto.setApiKey(rawApiKey);
        dto.setStatus("enabled");
        aiModelManageService.createModel(dto);

        // 1. 底层按指定版本解密测试
        AiModelConfigEntity entity = aiModelConfigDao.findByConfigName(TEST_MODEL_A);
        String decryptedKey = aiApiKeyCipherService.decrypt(entity.getApiKeyCipher(), entity.getKeyVersion());
        Assertions.assertEquals(rawApiKey, decryptedKey, "解密出的 API Key 必须与原始明文完全一致");

        // 2. 已保存模型的连通性测试
        AiModelTestResultVO savedResult = aiModelManageService.testSavedModel(TEST_MODEL_A);
        Assertions.assertNotNull(savedResult);
        Assertions.assertTrue(Boolean.TRUE.equals(savedResult.getSuccess()), "已保存模型连通性测试应成功通过");

        // 3. 草稿模型的连通性测试（留空 apiKey 时自动回显已保存模型的解密 key）
        AiModelTestDTO draftDto = new AiModelTestDTO();
        draftDto.setName(TEST_MODEL_A);
        draftDto.setProvider("mock");
        draftDto.setConfigType("chat");
        draftDto.setModelName("mock-model-v1");
        draftDto.setApiKey(""); // 模拟前端留空编辑
        AiModelTestResultVO draftResult = aiModelManageService.testDraftModel(draftDto);
        Assertions.assertNotNull(draftResult);
        Assertions.assertTrue(Boolean.TRUE.equals(draftResult.getSuccess()), "草稿模型在留空 API Key 时应能复用已保存模型的解密凭证");
    }

    @Test
    @DisplayName("Gate I10-4: KMS 密钥轮换生命周期与历史旧版本解密兼容")
    public void testRotateModelKeyLifecycleAndLegacyDecrypt() {
        // 1. 在 v1 版本下创建模型 A
        String rawKeyA = "sk-gate-i10-model-a-v1-key";
        AiModelSaveDTO dtoA = new AiModelSaveDTO();
        dtoA.setName(TEST_MODEL_A);
        dtoA.setProvider("mock");
        dtoA.setConfigType("chat");
        dtoA.setModelName("mock-model-v1");
        dtoA.setApiKey(rawKeyA);
        dtoA.setStatus("enabled");
        aiModelManageService.createModel(dtoA);

        AiModelConfigEntity entityA = aiModelConfigDao.findByConfigName(TEST_MODEL_A);
        Assertions.assertEquals(1, entityA.getKeyVersion(), "模型 A 应记录 key_version = 1");

        // 2. 模拟 KMS 轮换：将 edumind-model-key 轮换至 v2
        SecurityKeyRotateDTO rotateDTO = new SecurityKeyRotateDTO();
        rotateDTO.setKeyAlias(MODEL_KEY_ALIAS);
        SecurityKeyVersionVO rotated = securityKeyVersionService.rotateKey(rotateDTO);
        Assertions.assertEquals(2, rotated.getKeyVersion(), "轮换后生成的新版本应为 2");
        Assertions.assertEquals("ACTIVE", rotated.getStatus());

        int currentActiveVer = securityKeyQueryApi.getActiveKeyVersion(PLATFORM_TENANT, MODEL_KEY_ALIAS);
        Assertions.assertEquals(2, currentActiveVer, "KMS 当前有效版本应变为 2");

        // 3. 在 v2 版本下创建模型 B
        String rawKeyB = "sk-gate-i10-model-b-v2-key";
        AiModelSaveDTO dtoB = new AiModelSaveDTO();
        dtoB.setName(TEST_MODEL_B);
        dtoB.setProvider("mock");
        dtoB.setConfigType("chat");
        dtoB.setModelName("mock-model-v2");
        dtoB.setApiKey(rawKeyB);
        dtoB.setStatus("enabled");
        aiModelManageService.createModel(dtoB);

        AiModelConfigEntity entityB = aiModelConfigDao.findByConfigName(TEST_MODEL_B);
        Assertions.assertEquals(2, entityB.getKeyVersion(), "模型 B 必须绑定新的有效版本 key_version = 2");

        // 4. 双向解密验证：历史模型 A (v1) 与最新模型 B (v2) 均能根据实体自身记录的版本无缝解密
        String decryptedA = aiApiKeyCipherService.decrypt(entityA.getApiKeyCipher(), entityA.getKeyVersion());
        Assertions.assertEquals(rawKeyA, decryptedA, "密钥轮换后，携带历史 keyVersion=1 依然能准确解密旧密文");

        String decryptedB = aiApiKeyCipherService.decrypt(entityB.getApiKeyCipher(), entityB.getKeyVersion());
        Assertions.assertEquals(rawKeyB, decryptedB, "新模型使用 v2 密钥能准确解密");
    }

    @Test
    @DisplayName("Gate I10-5: 遗留旧密钥回退 (Layer 2) 与明文容错 (Layer 3) 三层平滑回退测试")
    public void testLegacySecretFallback() throws Exception {
        // 1. 模拟遗留系统使用 model-key-secret 派生的密钥加密历史数据
        String legacyRawKey = "sk-legacy-pre-kms-token-2025";
        String legacyKey16 = resolveLegacyKey16();
        String legacyCipher = sm4Service.encryptToBase64(legacyKey16, legacyRawKey);

        // 传入一个在 KMS 中不存在或不匹配的密钥版本 (如 v999)，触发 Layer 1 失败并顺利降级到 Layer 2 遗留密钥解密
        String decryptedLegacy = aiApiKeyCipherService.decrypt(legacyCipher, 999);
        Assertions.assertEquals(legacyRawKey, decryptedLegacy, "Layer 2 回退解密应成功还原使用遗留 secret 加密的历史数据");

        // 不传 keyVersion (null) 时同样能回退解密
        String decryptedLegacyWithoutVer = aiApiKeyCipherService.decrypt(legacyCipher);
        Assertions.assertEquals(legacyRawKey, decryptedLegacyWithoutVer, "缺省 keyVersion 时也应能平滑回退解密");

        // 2. 模拟本地开发或测试环境直接填入的明文容错 (Layer 3)
        String plainSk = "sk-developer-direct-plaintext-key";
        Assertions.assertEquals(plainSk, aiApiKeyCipherService.decrypt(plainSk, 1), "Layer 3 前置容错：以 sk- 开头的明文直接透传");

        String bearerToken = "Bearer test-direct-bearer-token-abc";
        Assertions.assertEquals(bearerToken, aiApiKeyCipherService.decrypt(bearerToken, 1), "Layer 3 前置容错：以 Bearer 开头的明文直接透传");

        String randomPlaintext = "raw-unencrypted-legacy-string";
        Assertions.assertEquals(randomPlaintext, aiApiKeyCipherService.decrypt(randomPlaintext, 1), "Layer 3 后置容错：无法解析的密文最终降级返回原字符串");
    }

    @Test
    @DisplayName("Gate I10-6: LlmClientRegistry 运行时通过 KMS 解密并动态构建客户端")
    public void testLlmClientRegistryRuntimeDecrypt() {
        String rawApiKey = "sk-gate-i10-registry-runtime-key";
        AiModelSaveDTO dto = new AiModelSaveDTO();
        dto.setName(TEST_MODEL_A);
        dto.setProvider("mock");
        dto.setConfigType("chat");
        dto.setModelName("mock-model-v1");
        dto.setApiKey(rawApiKey);
        dto.setStatus("enabled");
        aiModelManageService.createModel(dto);

        // 刷新并获取 client 实例
        llmClientRegistry.invalidateAll();
        LlmClient client1 = llmClientRegistry.get(TEST_MODEL_A);
        Assertions.assertNotNull(client1, "LlmClientRegistry 必须能成功解析并构建模型 client");

        // 局部失效与重新拉取
        llmClientRegistry.invalidate(TEST_MODEL_A);
        LlmClient client2 = llmClientRegistry.get(TEST_MODEL_A);
        Assertions.assertNotNull(client2, "缓存失效后重新构建 client 成功");
    }

    private String resolveLegacyKey16() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(modelKeySecret.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append((char) ('a' + (hash[i] & 0x0F)));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "EduMindAIKey16!!";
        }
    }
}
