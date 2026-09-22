package com.edumind.ai.migration;

import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.integration.crypto.AiApiKeyCipherService;
import com.edumind.ai.service.memory.MemoryCryptoService;
import com.edumind.security.crypto.Sm4Service;
import com.edumind.system.api.SecurityKeyQueryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 国密密钥派生算法（KDF）加固迁移任务。
 *
 * <p>背景：加固前的 KDF 把哈希字节映射到 'a'~'p'，SM4 密钥有效熵只有 64 bit；
 * 加固后新加密一律使用约 125 bit 熵的新 KDF，但库中已存在的密文仍是旧 KDF 产物。</p>
 *
 * <p>处理策略（幂等）：
 * <ul>
 *   <li>新 KDF 能解开 → 已是加固后的密文，跳过；</li>
 *   <li>仅旧 KDF 能解开 → 用新 KDF 重新加密并回写密文与新版本号；</li>
 *   <li>两套都解不开 → 非 KMS 密文（如遗留明文/第三方加密），保持原样不动。</li>
 * </ul>
 *
 * <p>任务在启动完成后异步触发，失败只记录日志（下次启动自动重试），不会阻塞启动；
 * 且读取路径本身已兼容新旧两套 KDF，因此即使迁移未执行也不会解密失败。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edumind.security.kdf-migration", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CipherKdfMigrationRunner {

    private static final int BATCH_SIZE = 200;

    /** 模型密钥为平台级资产，与 AiApiKeyCipherService 保持一致使用租户 1 */
    private static final Long PLATFORM_TENANT_ID = 1L;

    private final AiMemoryDao aiMemoryDao;
    private final AiModelConfigDao aiModelConfigDao;
    private final MemoryCryptoService memoryCryptoService;
    private final AiApiKeyCipherService aiApiKeyCipherService;
    private final Sm4Service sm4Service;

    /** KMS 门面可能不存在（离线降级部署），此时跳过迁移 */
    @Autowired(required = false)
    private SecurityKeyQueryApi securityKeyQueryApi;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String dataKeyAlias;

    @Value("${edumind.security.model-key-alias:edumind-model-key}")
    private String modelKeyAlias;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateLegacyCiphertext() {
        if (securityKeyQueryApi == null) {
            log.info("[KMS KDF 迁移] 未启用 KMS 门面，跳过存量密文迁移");
            return;
        }
        long start = System.currentTimeMillis();
        try {
            int memoryMigrated = migrateMemoryItems();
            int modelMigrated = migrateModelApiKeys();
            long cost = System.currentTimeMillis() - start;
            if (memoryMigrated + modelMigrated > 0) {
                log.info("[KMS KDF 迁移] 完成：长期记忆 {} 条、模型 API Key {} 条已用新 KDF 重新加密，耗时 {} ms",
                        memoryMigrated, modelMigrated, cost);
            } else {
                log.info("[KMS KDF 迁移] 无需迁移（存量密文已是新 KDF 或非 KMS 密文），耗时 {} ms", cost);
            }
        } catch (Exception ex) {
            log.error("[KMS KDF 迁移] 执行异常，已跳过（读取路径兼容新旧 KDF，不影响使用；下次启动自动重试）", ex);
        }
    }

    /** 迁移长期记忆密文（ai_memory_item.content_ciphertext） */
    private int migrateMemoryItems() {
        int migrated = 0;
        long lastId = 0L;
        while (true) {
            List<AiMemoryItemEntity> batch = aiMemoryDao.listItemsWithCiphertextAfterId(lastId, BATCH_SIZE);
            if (CollectionUtils.isEmpty(batch)) {
                return migrated;
            }
            for (AiMemoryItemEntity item : batch) {
                lastId = Math.max(lastId, item.getId());
                if (reEncryptMemoryItem(item)) {
                    migrated++;
                }
            }
            if (batch.size() < BATCH_SIZE) {
                return migrated;
            }
        }
    }

    private boolean reEncryptMemoryItem(AiMemoryItemEntity item) {
        int version = normalizeVersion(item.getKeyVersion());
        String plain = decryptWhenLegacyEncrypted(item.getContentCiphertext(), item.getTenantId(), dataKeyAlias, version);
        if (plain == null) {
            return false;
        }
        MemoryCryptoService.EncryptResult result = memoryCryptoService.encryptWithVersion(item.getTenantId(), plain);
        if (result == null || !StringUtils.hasText(result.getCiphertext())) {
            return false;
        }
        item.setContentCiphertext(result.getCiphertext());
        item.setKeyVersion(result.getKeyVersion());
        aiMemoryDao.updateItem(item);
        return true;
    }

    /** 迁移模型 API Key 密文（ai_model_config.api_key_cipher） */
    private int migrateModelApiKeys() {
        int migrated = 0;
        for (AiModelConfigEntity config : aiModelConfigDao.listAll()) {
            if (!StringUtils.hasText(config.getApiKeyCipher())) {
                continue;
            }
            int version = normalizeVersion(config.getKeyVersion());
            String plain = decryptWhenLegacyEncrypted(
                    config.getApiKeyCipher(), PLATFORM_TENANT_ID, modelKeyAlias, version);
            if (plain == null) {
                continue;
            }
            AiApiKeyCipherService.EncryptResult result = aiApiKeyCipherService.encryptWithVersion(plain);
            if (result == null || !StringUtils.hasText(result.getCiphertext())) {
                continue;
            }
            config.setApiKeyCipher(result.getCiphertext());
            config.setKeyVersion(result.getKeyVersion());
            aiModelConfigDao.updateById(config);
            migrated++;
        }
        return migrated;
    }

    /**
     * 仅当密文「新 KDF 解不开、旧 KDF 能解开」时返回明文（表示需要重加密）；
     * 新 KDF 已能解开（幂等跳过）或两套 KDF 都解不开（非 KMS 密文）时返回 null。
     */
    private String decryptWhenLegacyEncrypted(String cipherText, Long tenantId, String alias, int version) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        String cipher = cipherText.trim();
        List<String> candidates = securityKeyQueryApi.resolveDataKeyCandidates(tenantId, alias, version);
        try {
            // 新 KDF 能解开说明已是加固后的密文，直接跳过（保证任务幂等）
            sm4Service.decryptFromBase64(candidates.get(0), cipher);
            return null;
        } catch (Exception ignored) {
            // 继续尝试旧 KDF
        }
        for (int i = 1; i < candidates.size(); i++) {
            try {
                return sm4Service.decryptFromBase64(candidates.get(i), cipher);
            } catch (Exception ignored) {
                // 尝试下一个候选密钥
            }
        }
        return null;
    }

    private static int normalizeVersion(Integer keyVersion) {
        return (keyVersion != null && keyVersion > 0) ? keyVersion : 1;
    }
}
