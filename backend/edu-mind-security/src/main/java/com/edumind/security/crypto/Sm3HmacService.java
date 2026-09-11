package com.edumind.security.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * 国密 SM3-HMAC 消息认证码服务
 * 用于接口数据签名、参数完整性防篡改校验
 */
@Service
public class Sm3HmacService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 计算 SM3-HMAC 摘要字节数组
     *
     * @param key   密钥
     * @param data  输入数据
     * @return HMAC-SM3 摘要字节
     */
    public byte[] hmac(byte[] key, byte[] data) {
        HMac hmac = new HMac(new SM3Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(data, 0, data.length);
        byte[] result = new byte[hmac.getMacSize()];
        hmac.doFinal(result, 0);
        return result;
    }

    /**
     * 计算 SM3-HMAC 摘要（返回 64 位 Hex 字符串）
     *
     * @param secretKey 字符串密钥
     * @param content   明文字符串
     * @return 16 进制字符串签名
     */
    public String hmacHex(String secretKey, String content) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] macBytes = hmac(keyBytes, dataBytes);
        return Hex.toHexString(macBytes);
    }

    /**
     * 校验 SM3-HMAC 签名是否合法
     *
     * @param secretKey     密钥
     * @param content       内容
     * @param expectedSign  期望的签名（16进制）
     * @return 是否一致
     */
    public boolean verify(String secretKey, String content, String expectedSign) {
        if (secretKey == null || content == null || expectedSign == null) {
            return false;
        }
        String calculated = hmacHex(secretKey, content);
        return calculated.equalsIgnoreCase(expectedSign);
    }
}
