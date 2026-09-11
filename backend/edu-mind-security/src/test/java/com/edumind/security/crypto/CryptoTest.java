package com.edumind.security.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class CryptoTest {

    private final Sm3HmacService sm3HmacService = new Sm3HmacService();
    private final Sm4GcmService sm4GcmService = new Sm4GcmService();
    private final SignatureService signatureService = new SignatureService(sm3HmacService);

    @Test
    public void testSm3Hmac() {
        String key = "EduMind_Secret_Key_123456";
        String content = "GET\n/api/courses/1\n1726021200000\nnonce_test_abc123\n";

        String hmac1 = sm3HmacService.hmacHex(key, content);
        String hmac2 = sm3HmacService.hmacHex(key, content);

        Assertions.assertNotNull(hmac1);
        Assertions.assertEquals(64, hmac1.length()); // 256-bit SM3 digest = 64 hex characters
        Assertions.assertEquals(hmac1, hmac2);
        Assertions.assertTrue(sm3HmacService.verify(key, content, hmac1));
    }

    @Test
    public void testSm4Gcm() throws Exception {
        String key16 = "1234567890123456"; // 16 bytes key
        String plainText = "Hello EduMind! 这是国密 SM4-GCM 认证加密测试数据。";

        String cipherBase64 = sm4GcmService.encryptToBase64(key16, plainText);
        Assertions.assertNotNull(cipherBase64);

        String decrypted = sm4GcmService.decryptFromBase64(key16, cipherBase64);
        Assertions.assertEquals(plainText, decrypted);
    }

    @Test
    public void testSignatureService() {
        String secretKey = "test_app_secret";
        long timestamp = System.currentTimeMillis();
        String nonce = "random_nonce_999";
        String path = "/api/v1/ai/chat";
        String method = "POST";
        String body = "{\"message\":\"hello\"}";

        String sign = signatureService.generateSignature(method, path, timestamp, nonce, body, secretKey);
        Assertions.assertNotNull(sign);

        boolean ok = signatureService.verifySignature(method, path, timestamp, nonce, body, secretKey, sign);
        Assertions.assertTrue(ok);

        // Tamper test
        boolean tampered = signatureService.verifySignature(method, path, timestamp, nonce, body + "tampered", secretKey, sign);
        Assertions.assertFalse(tampered);
    }
}
