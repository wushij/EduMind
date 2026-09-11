package com.edumind.security.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 国密 SM3 服务门面（主要承载 SM3-HMAC 消息认证能力）
 */
@Service
@RequiredArgsConstructor
public class Sm3Service {

    private final Sm3HmacService sm3HmacService;

    public String hmacHex(String key, String content) {
        return sm3HmacService.hmacHex(key, content);
    }

    public boolean verify(String key, String content, String sign) {
        return sm3HmacService.verify(key, content, sign);
    }
}