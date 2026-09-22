package com.edumind.system.service.security.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.system.dao.security.SecurityKeyVersionDao;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.service.security.SecurityKeyQueryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityKeyQueryServiceImpl implements SecurityKeyQueryService {

    /** 仓库内置演示主密钥：仍在使用时告警，生产可通过 reject-demo-master-secret 直接拒绝启动 */
    private static final String DEMO_MASTER_SECRET = "EduMind_Memory_Key_Seed_2026";

    /** 新 KDF 字符取值范围（0x20~0xFF 共 224 个）：每字符恰好 1 字节且避开控制字符 */
    private static final int KEY_CHAR_RANGE = 0xE0;

    /** SM4 密钥字节长度 */
    private static final int KEY_LENGTH = 16;

    private final SecurityKeyVersionDao securityKeyVersionDao;

    /**
     * 国密主密钥：不再提供代码内置默认值。
     * 缺失时启动即失败（Fail-Fast），生产通过环境变量 EDUMIND_SECURITY_MASTER_SECRET 注入。
     */
    @Value("${edumind.security.master-secret:}")
    private String masterSecret;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String defaultKeyAlias;

    /** true=检测到演示主密钥时拒绝启动（生产建议开启） */
    @Value("${edumind.security.reject-demo-master-secret:false}")
    private boolean rejectDemoMasterSecret;

    @PostConstruct
    void validateMasterSecret() {
        if (!StringUtils.hasText(masterSecret)) {
            throw new IllegalStateException("缺少国密主密钥配置 edumind.security.master-secret，"
                    + "请通过环境变量 EDUMIND_SECURITY_MASTER_SECRET 注入，代码不再提供内置默认值");
        }
        masterSecret = masterSecret.trim();
        if (DEMO_MASTER_SECRET.equals(masterSecret)) {
            if (rejectDemoMasterSecret) {
                throw new IllegalStateException("检测到内置演示主密钥，当前环境禁止启动："
                        + "请注入随机主密钥 EDUMIND_SECURITY_MASTER_SECRET");
            }
            log.warn("[KMS 安全告警] 正在使用内置演示主密钥，仅限开发/演示环境；"
                    + "生产环境必须通过环境变量 EDUMIND_SECURITY_MASTER_SECRET 注入随机密钥");
        }
    }

    @Override
    public int getActiveKeyVersion(Long tenantId, String keyAlias) {
        Long tid = tenantId != null ? tenantId : TenantContext.getTenantId();
        if (tid == null) {
            tid = 1L;
        }
        String alias = StringUtils.hasText(keyAlias) ? keyAlias : defaultKeyAlias;
        SecurityKeyVersionEntity active = securityKeyVersionDao.findActiveByTenantAndAlias(tid, alias);
        if (active != null && active.getKeyVersion() != null) {
            return active.getKeyVersion();
        }
        // 懒初始化 v1
        SecurityKeyVersionEntity v1 = new SecurityKeyVersionEntity();
        v1.setTenantId(tid);
        v1.setKeyAlias(alias);
        v1.setKeyVersion(1);
        v1.setAlgorithm("SM4_GCM");
        v1.setStatus("ACTIVE");
        v1.setActivatedTime(LocalDateTime.now());
        try {
            securityKeyVersionDao.insert(v1);
            log.info("[KMS API] 租户 {} 别名 {} 懒初始化密钥版本 v1 成功", tid, alias);
        } catch (Exception e) {
            log.debug("[KMS API] 并发初始化或已有记录: {}", e.getMessage());
            SecurityKeyVersionEntity secondCheck = securityKeyVersionDao.findActiveByTenantAndAlias(tid, alias);
            if (secondCheck != null && secondCheck.getKeyVersion() != null) {
                return secondCheck.getKeyVersion();
            }
        }
        return 1;
    }

    @Override
    public String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion) {
        return deriveDataKey16(tenantId, keyAlias, keyVersion, false);
    }

    @Override
    public String resolveLegacyDataKey16(Long tenantId, String keyAlias, int keyVersion) {
        return deriveDataKey16(tenantId, keyAlias, keyVersion, true);
    }

    @Override
    public List<String> resolveDataKeyCandidates(Long tenantId, String keyAlias, int keyVersion) {
        String primary = resolveDataKey16(tenantId, keyAlias, keyVersion);
        String legacy = resolveLegacyDataKey16(tenantId, keyAlias, keyVersion);
        return primary.equals(legacy) ? List.of(primary) : List.of(primary, legacy);
    }

    /**
     * 派生 16 字节 SM4 密钥材料。
     *
     * <p>2026-09 安全加固：旧实现为 {@code (char)('a' + (hash[i] & 0x0F))}，
     * 每字节仅 16 种取值（4 bit 熵），16 字节密钥的有效强度被压缩到 64 bit；
     * 新实现把哈希字节映射到 Latin-1 区间 0x20~0xFF，每字符恰好 1 字节，
     * 密钥熵提升到约 125 bit。</p>
     *
     * @param legacy true=使用加固前的旧 KDF（仅用于解密存量密文）
     */
    private String deriveDataKey16(Long tenantId, String keyAlias, int keyVersion, boolean legacy) {
        Long tid = tenantId != null ? tenantId : 0L;
        try {
            String seed;
            boolean isCustomAlias = StringUtils.hasText(keyAlias) && !defaultKeyAlias.equals(keyAlias);
            String aliasPart = isCustomAlias ? "_" + keyAlias : "";
            if (keyVersion <= 1) {
                // 默认别名保持与旧长期记忆数据派生兼容
                seed = masterSecret + "_" + tid + aliasPart;
            } else {
                seed = masterSecret + "_" + tid + aliasPart + "_v" + keyVersion;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(KEY_LENGTH);
            for (int i = 0; i < KEY_LENGTH; i++) {
                if (legacy) {
                    sb.append((char) ('a' + (hash[i] & 0x0F)));
                } else {
                    sb.append((char) (0x20 + ((hash[i] & 0xFF) % KEY_CHAR_RANGE)));
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            // Fail-Fast：不再回退到硬编码密钥（故障时用固定密钥加密等于没有加密）
            throw new IllegalStateException("国密密钥材料派生失败: " + ex.getMessage(), ex);
        }
    }
}
