package com.edumind.ai.integration.crypto;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiApiKeyCipherService {

    private final Sm4Service sm4Service;

    @Autowired(required = false)
    private SecurityKeyQueryApi securityKeyQueryApi;

    @Value("${edumind.security.model-key-alias:edumind-model-key}")
    private String modelKeyAlias;

    @Value("${edumind.ai.model-key-secret:EduMind_AI_Model_KEK}")
    private String modelKeySecret;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EncryptResult {
        private String ciphertext;
        private int keyVersion;
    }

    /**
     * 使用 KMS 当前 ACTIVE 密钥版本加密 API Key 并返回密文及版本号
     */
    public EncryptResult encryptWithVersion(String plainApiKey) {
        if (!StringUtils.hasText(plainApiKey)) {
            return null;
        }
        try {
            int keyVersion = 1;
            String key16;
            if (securityKeyQueryApi != null) {
                keyVersion = securityKeyQueryApi.getActiveKeyVersion(1L, modelKeyAlias);
                key16 = securityKeyQueryApi.resolveDataKey16(1L, modelKeyAlias, keyVersion);
            } else {
                key16 = resolveLegacyKey16();
            }
            String cipher = sm4Service.encryptToBase64(key16, plainApiKey.trim());
            return new EncryptResult(cipher, keyVersion);
        } catch (Exception ex) {
            log.error("AI API Key encrypt failed", ex);
            throw new IllegalStateException("API Key 加密失败", ex);
        }
    }

    /**
     * 兼容旧接口，直接返回密文字符串
     */
    public String encrypt(String plainApiKey) {
        EncryptResult result = encryptWithVersion(plainApiKey);
        return result != null ? result.getCiphertext() : null;
    }

    /**
     * 按指定 KMS 密钥版本解密，支持 3 层容错：
     * 1. 优先使用 KMS 指定版本密钥材料解密
     * 2. 失败时降级使用遗留 modelKeySecret 派生旧密钥解密
     * 3. 仍失败或以 sk-/Bearer 开头时，直接返回明文容错
     */
    public String decrypt(String cipherText, Integer keyVersion) {
        if (!StringUtils.hasText(cipherText)) {
            return "";
        }
        String trimmed = cipherText.trim();
        // 第3层前置容错：若是显式明文（如以 sk- 或 Bearer 开头），直接返回
        if (trimmed.startsWith("sk-") || trimmed.startsWith("Bearer ")) {
            return trimmed;
        }

        // 第1层：优先使用 KMS 指定版本或 active 版本解密
        int ver = (keyVersion != null && keyVersion > 0)
                ? keyVersion
                : (securityKeyQueryApi != null ? securityKeyQueryApi.getActiveKeyVersion(1L, modelKeyAlias) : 1);
        if (securityKeyQueryApi != null) {
            try {
                String kmsKey16 = securityKeyQueryApi.resolveDataKey16(1L, modelKeyAlias, ver);
                return sm4Service.decryptFromBase64(kmsKey16, trimmed);
            } catch (Exception ex) {
                log.debug("[AI Key KMS] Versioned decrypt with v{} failed, trying fallback: {}", ver, ex.getMessage());
            }
        }

        // 第2层：若 KMS 解密失败，尝试使用遗留的 modelKeySecret 派生旧密钥解密（平滑兼容历史数据）
        try {
            return sm4Service.decryptFromBase64(resolveLegacyKey16(), trimmed);
        } catch (Exception ex) {
            log.debug("[AI Key KMS] Legacy secret decrypt failed: {}", ex.getMessage());
        }

        // 第3层后置容错：若非标准密文或为遗留明文，降级返回 trimmed
        log.warn("[AI Key KMS] AI API Key decrypt failed, treat as legacy plaintext");
        return trimmed;
    }

    /**
     * 兼容旧接口，默认尝试 active 或 v1 解密并走 3 层回退
     */
    public String decrypt(String cipherText) {
        return decrypt(cipherText, null);
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
