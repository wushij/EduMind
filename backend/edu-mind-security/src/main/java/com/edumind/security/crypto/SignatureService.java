package com.edumind.security.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 接口防篡改签名服务
 * 基于 时间戳 (Timestamp) + 随机数 (Nonce) + 国密 SM3-HMAC 实现
 */
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final Sm3HmacService sm3HmacService;

    /**
     * 构建待签名规范字符串 (Canonical Sign String)
     * 规范格式：HTTP_METHOD + "\n" + REQUEST_PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY
     */
    public String buildCanonicalString(String method, String path, long timestamp, String nonce, String body) {
        return (method != null ? method.toUpperCase() : "GET") + "\n"
                + (path != null ? path : "/") + "\n"
                + timestamp + "\n"
                + (nonce != null ? nonce : "") + "\n"
                + (body != null ? body : "");
    }

    /**
     * 基于 SM3-HMAC 生成请求签名
     */
    public String generateSignature(String method, String path, long timestamp, String nonce, String body, String secretKey) {
        String canonicalString = buildCanonicalString(method, path, timestamp, nonce, body);
        return sm3HmacService.hmacHex(secretKey, canonicalString);
    }

    /**
     * 校验请求签名是否合法
     */
    public boolean verifySignature(String method, String path, long timestamp, String nonce, String body, String secretKey, String signature) {
        if (signature == null || secretKey == null) {
            return false;
        }
        String expectedSignature = generateSignature(method, path, timestamp, nonce, body, secretKey);
        return expectedSignature.equalsIgnoreCase(signature);
    }
}