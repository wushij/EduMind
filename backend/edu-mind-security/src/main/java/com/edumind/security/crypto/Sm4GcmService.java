package com.edumind.security.crypto;

import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * 国密 SM4-GCM 认证加解密服务 (AEAD 模式)
 * 用于高敏感数据（如用户敏感凭证、模型 API Key 等）的加密传输与存储。
 * 具备机密性保障与防止重放、篡改、密文填充攻击能力。
 */
@Service
public class Sm4GcmService {

    private static final int TAG_BIT_LENGTH = 128; // 认证标签长度 128 位 (16 字节)
    public static final int DEFAULT_IV_LENGTH = 12; // GCM 模式推荐 96 位 (12 字节) IV

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 生成安全的随机 IV (12 字节)
     */
    public byte[] generateIv() {
        byte[] iv = new byte[DEFAULT_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * SM4-GCM 认证加密
     *
     * @param key        16 字节密钥
     * @param iv         12 字节向量
     * @param plainBytes 明文字节
     * @param aad        关联认证数据 (可为 null)
     * @return 包含认证 Tag 的密文字节
     */
    public byte[] encrypt(byte[] key, byte[] iv, byte[] plainBytes, byte[] aad) throws Exception {
        GCMModeCipher cipher = GCMBlockCipher.newInstance(new SM4Engine());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), TAG_BIT_LENGTH, iv, aad);
        cipher.init(true, parameters);

        byte[] out = new byte[cipher.getOutputSize(plainBytes.length)];
        int len = cipher.processBytes(plainBytes, 0, plainBytes.length, out, 0);
        cipher.doFinal(out, len);
        return out;
    }

    /**
     * SM4-GCM 认证解密
     *
     * @param key         16 字节密钥
     * @param iv          12 字节向量
     * @param cipherBytes 包含认证 Tag 的密文字节
     * @param aad         关联认证数据 (可为 null，必须与加密时一致)
     * @return 解密后的明文字节
     */
    public byte[] decrypt(byte[] key, byte[] iv, byte[] cipherBytes, byte[] aad) throws Exception {
        GCMModeCipher cipher = GCMBlockCipher.newInstance(new SM4Engine());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), TAG_BIT_LENGTH, iv, aad);
        cipher.init(false, parameters);

        byte[] out = new byte[cipher.getOutputSize(cipherBytes.length)];
        int len = cipher.processBytes(cipherBytes, 0, cipherBytes.length, out, 0);
        cipher.doFinal(out, len);
        return out;
    }

    /**
     * 字符串加密（返回 Base64 编码，格式：Base64(IV + CipherWithTag)）
     */
    public String encryptToBase64(String secretKey16, String plainText) throws Exception {
        byte[] keyBytes = secretKey16.getBytes(StandardCharsets.UTF_8);
        byte[] iv = generateIv();
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherWithTag = encrypt(keyBytes, iv, plainBytes, null);

        // 拼接 iv (12 字节) + cipherWithTag
        byte[] combined = new byte[iv.length + cipherWithTag.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherWithTag, 0, combined, iv.length, cipherWithTag.length);

        return Base64.toBase64String(combined);
    }

    /**
     * 字符串解密（解析 Base64 编码，格式：Base64(IV + CipherWithTag)）
     */
    public String decryptFromBase64(String secretKey16, String base64Payload) throws Exception {
        byte[] combined = Base64.decode(base64Payload);
        if (combined.length < DEFAULT_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid SM4-GCM payload length");
        }

        byte[] iv = new byte[DEFAULT_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, DEFAULT_IV_LENGTH);

        int cipherLen = combined.length - DEFAULT_IV_LENGTH;
        byte[] cipherWithTag = new byte[cipherLen];
        System.arraycopy(combined, DEFAULT_IV_LENGTH, cipherWithTag, 0, cipherLen);

        byte[] keyBytes = secretKey16.getBytes(StandardCharsets.UTF_8);
        byte[] plainBytes = decrypt(keyBytes, iv, cipherWithTag, null);

        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
