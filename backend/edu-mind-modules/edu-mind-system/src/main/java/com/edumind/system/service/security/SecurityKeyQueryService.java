package com.edumind.system.service.security;

import java.util.List;

public interface SecurityKeyQueryService {

    int getActiveKeyVersion(Long tenantId, String keyAlias);

    /**
     * 派生 16 字节 SM4 密钥材料（128 bit 熵），用于新的加密写入。
     */
    String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion);

    /**
     * 加固前的旧 KDF 派生（每字节仅 4 bit 熵），仅用于解密加固前产生的存量密文。
     */
    String resolveLegacyDataKey16(Long tenantId, String keyAlias, int keyVersion);

    /**
     * 解密候选密钥列表（新 KDF 在前、旧 KDF 在后），调用方按顺序尝试即可兼容存量密文。
     */
    List<String> resolveDataKeyCandidates(Long tenantId, String keyAlias, int keyVersion);
}
