package com.edumind.security.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.Security;

/**
 * 国密 SM2 非对称密码算法服务
 * 用于非对称公私钥加密、数字签名与验签
 */
@Service
public class Sm2Service {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
