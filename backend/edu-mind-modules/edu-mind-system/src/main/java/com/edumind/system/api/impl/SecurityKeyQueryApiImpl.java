package com.edumind.system.api.impl;

import com.edumind.system.api.SecurityKeyQueryApi;
import com.edumind.system.service.security.SecurityKeyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 国密 KMS 密钥跨模块门面实现
 */
@Service
@RequiredArgsConstructor
public class SecurityKeyQueryApiImpl implements SecurityKeyQueryApi {

    private final SecurityKeyQueryService securityKeyQueryService;

    @Override
    public int getActiveKeyVersion(Long tenantId, String keyAlias) {
        return securityKeyQueryService.getActiveKeyVersion(tenantId, keyAlias);
    }

    @Override
    public String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion) {
        return securityKeyQueryService.resolveDataKey16(tenantId, keyAlias, keyVersion);
    }

    @Override
    public String resolveLegacyDataKey16(Long tenantId, String keyAlias, int keyVersion) {
        return securityKeyQueryService.resolveLegacyDataKey16(tenantId, keyAlias, keyVersion);
    }

    @Override
    public java.util.List<String> resolveDataKeyCandidates(Long tenantId, String keyAlias, int keyVersion) {
        return securityKeyQueryService.resolveDataKeyCandidates(tenantId, keyAlias, keyVersion);
    }
}
