package com.edumind.ai.integration.crypto;

import com.edumind.security.crypto.Sm4Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${edumind.ai.model-key-secret:EduMind_AI_Model_KEK}")
    private String modelKeySecret;

    public String encrypt(String plainApiKey) {
        if (!StringUtils.hasText(plainApiKey)) {
            return null;
        }
        try {
            return sm4Service.encryptToBase64(resolveKey16(), plainApiKey.trim());
        } catch (Exception ex) {
            log.error("AI API Key encrypt failed", ex);
            throw new IllegalStateException("API Key 加密失败");
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return "";
        }
        String trimmed = cipherText.trim();
        if (trimmed.startsWith("sk-") || trimmed.startsWith("Bearer ")) {
            return trimmed;
        }
        try {
            return sm4Service.decryptFromBase64(resolveKey16(), trimmed);
        } catch (Exception ex) {
            log.warn("AI API Key decrypt failed, treat as legacy plaintext");
            return trimmed;
        }
    }

    private String resolveKey16() {
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
