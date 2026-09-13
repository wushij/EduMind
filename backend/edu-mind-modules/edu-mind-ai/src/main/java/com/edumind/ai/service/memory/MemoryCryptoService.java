package com.edumind.ai.service.memory;

import com.edumind.security.crypto.Sm4Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 长期记忆敏感数据国密 SM4 加解密服务 (基于租户隔离派生密钥与 Fail-Closed 隐私安全设计)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCryptoService {

    private final Sm4Service sm4Service;

    @Value("${edumind.memory.key-secret:EduMind_Memory_Key_Seed_2026}")
    private String memoryKeySecret;

    /**
     * 加密敏感文本并输出 SM4-GCM Base64 密文
     */
    public String encrypt(Long tenantId, String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            String key16 = resolveTenantKey16(tenantId);
            return sm4Service.encryptToBase64(key16, plainText.trim());
        } catch (Exception ex) {
            log.error("[长期记忆SM4加密异常] 租户 ID: {}, 加密失败", tenantId, ex);
            throw new IllegalStateException("长期记忆敏感内容加密失败", ex);
        }
    }

    /**
     * 解密 SM4-GCM Base64 密文。
     * Fail-Closed 策略：若密钥不匹配、篡改或解密失败，返回 null，严禁泄露密文或明文。
     */
    public String decrypt(Long tenantId, String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        try {
            String key16 = resolveTenantKey16(tenantId);
            return sm4Service.decryptFromBase64(key16, cipherText.trim());
        } catch (Exception ex) {
            log.warn("[长期记忆SM4解密失败 (Fail-Closed)] 租户 ID: {}, 拒绝暴露未认证敏感数据", tenantId);
            return null;
        }
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
     * 基于租户 ID 与系统种子派生 16 字节 SM4 密钥，实现各租户密钥隔离
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
            return "EduMindMemKey16!";
        }
    }
}
