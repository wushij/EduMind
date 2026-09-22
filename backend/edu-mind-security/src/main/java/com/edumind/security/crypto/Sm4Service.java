package com.edumind.security.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 国密 SM4 服务门面（主要承载 SM4-GCM 认证加解密能力）
 */
@Service
@RequiredArgsConstructor
public class Sm4Service {

    private final Sm4GcmService sm4GcmService;

    public String encryptToBase64(String secretKey16, String plainText) throws Exception {
        return sm4GcmService.encryptToBase64(secretKey16, plainText);
    }

    public String decryptFromBase64(String secretKey16, String cipherText) throws Exception {
        return sm4GcmService.decryptFromBase64(secretKey16, cipherText);
    }

    /**
     * 依次使用多个候选密钥尝试解密，用于密钥派生算法（KDF）加固后的存量密文兼容。
     *
     * <p>SM4-GCM 是 AEAD 认证加密：密钥不正确时认证标签校验必定失败并抛异常，
     * 因此「新密钥失败 → 旧密钥成功」的判定是可靠的，不会把错误密钥解出的脏数据当成明文。</p>
     *
     * @param secretKeys 候选密钥，按优先级排列（新 KDF 密钥在前）
     * @return 解密后的明文
     * @throws Exception 所有候选密钥均解密失败时抛出最后一次异常
     */
    public String decryptTryingKeys(List<String> secretKeys, String cipherText) throws Exception {
        Exception lastError = null;
        if (secretKeys != null) {
            for (String secretKey : secretKeys) {
                if (!StringUtils.hasText(secretKey)) {
                    continue;
                }
                try {
                    return sm4GcmService.decryptFromBase64(secretKey, cipherText);
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
        }
        throw lastError != null ? lastError : new IllegalArgumentException("没有可用的 SM4 解密密钥");
    }
}