package com.edumind.security.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}