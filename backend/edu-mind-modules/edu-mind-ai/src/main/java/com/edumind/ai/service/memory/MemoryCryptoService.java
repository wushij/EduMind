package com.edumind.ai.service.memory;

import com.edumind.security.crypto.Sm4Service;
import com.edumind.system.api.SecurityKeyQueryApi;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 长期记忆敏感数据国密 SM4 加解密服务 (集成 KMS 密钥版本追踪与 Fail-Closed 隐私安全设计)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCryptoService {

    private final Sm4Service sm4Service;

    @Autowired(required = false)
    private SecurityKeyQueryApi securityKeyQueryApi;

    @Value("${edumind.memory.key-secret:${edumind.security.master-secret:EduMind_Memory_Key_Seed_2026}}")
    private String memoryKeySecret;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String defaultKeyAlias;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EncryptResult {
        private String ciphertext;
        private int keyVersion;
    }

    /**
     * 加密敏感文本并输出 SM4-GCM Base64 密文（兼容旧接口，直接返回密文）
     */
    public String encrypt(Long tenantId, String plainText) {
        EncryptResult result = encryptWithVersion(tenantId, plainText);
        return result != null ? result.getCiphertext() : null;
    }

    /**
     * 加密敏感文本并记录生效的 KMS 密钥版本号
     */
    public EncryptResult encryptWithVersion(Long tenantId, String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            int keyVersion = 1;
            String key16;
            if (securityKeyQueryApi != null) {
                keyVersion = securityKeyQueryApi.getActiveKeyVersion(tenantId, defaultKeyAlias);
                key16 = securityKeyQueryApi.resolveDataKey16(tenantId, defaultKeyAlias, keyVersion);
            } else {
                key16 = resolveTenantKey16(tenantId);
            }
            String cipher = sm4Service.encryptToBase64(key16, plainText.trim());
            return new EncryptResult(cipher, keyVersion);
        } catch (Exception ex) {
            log.error("[长期记忆SM4加密异常] 租户 ID: {}, 加密失败", tenantId, ex);
            throw new IllegalStateException("长期记忆敏感内容加密失败", ex);
        }
    }

    /**
     * 按指定 KMS 密钥版本解密 SM4-GCM Base64 密文。
     * 若 keyVersion 为 null 或 <= 0，自动降级为 v1。
     * Fail-Closed 策略：若密钥不匹配、篡改或解密失败，返回 null，严禁泄露密文或明文。
     */
    public String decrypt(Long tenantId, String cipherText, Integer keyVersion) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        int effectiveVersion = (keyVersion != null && keyVersion > 0) ? keyVersion : 1;
        try {
            // 先试 KDF 加固后的新密钥，失败再回退加固前的旧密钥（保证历史密文仍可解密）
            return sm4Service.decryptTryingKeys(resolveKeyCandidates(tenantId, effectiveVersion), cipherText.trim());
        } catch (Exception ex) {
            log.warn("[长期记忆SM4解密失败 (Fail-Closed)] 租户 ID: {}, 密钥版本: v{}, 拒绝暴露未认证敏感数据",
                    tenantId, effectiveVersion);
            return null;
        }
    }

    /**
     * 解密候选密钥列表：新 KDF 密钥在前、旧 KDF 密钥在后。
     * 旧 KDF 分支只用于兼容加固前写入的存量密文，不参与任何加密。
     */
    private List<String> resolveKeyCandidates(Long tenantId, int keyVersion) {
        if (securityKeyQueryApi != null) {
            return securityKeyQueryApi.resolveDataKeyCandidates(tenantId, defaultKeyAlias, keyVersion);
        }
        return List.of(resolveTenantKey16(tenantId));
    }

    /**
     * 兼容接口：默认以 v1 版本尝试解密
     */
    public String decrypt(Long tenantId, String cipherText) {
        return decrypt(tenantId, cipherText, 1);
    }

    /**
     * 针对高敏条目自动生成脱敏摘要，避免明文落盘泄密
     */
    public String buildDesensitizedSummary(String rawSummary, String sensitivityLevel) {
        if (!StringUtils.hasText(rawSummary)) {
            return "【无摘要内容】";
        }
        if ("HIGH_RISK".equalsIgnoreCase(sensitivityLevel)) {
            String preview = rawSummary.length() > 16 ? rawSummary.substring(0, 16) + "..." : rawSummary;
            return "【高敏记忆已国密加密】" + preview;
        }
        return rawSummary;
    }

    /**
     * 基于租户 ID 与系统种子派生 16 字节 SM4 密钥（离线容灾回退）
     */
    private String resolveTenantKey16(Long tenantId) {
        try {
            String seed = memoryKeySecret + "_" + (tenantId != null ? tenantId : 0L);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append((char) ('a' + (hash[i] & 0x0F)));
            }
            return sb.toString();
        } catch (Exception ex) {
            // Fail-Fast：不再回退到硬编码密钥
            throw new IllegalStateException("离线派生长期记忆SM4密钥失败: " + ex.getMessage(), ex);
        }
    }
}
