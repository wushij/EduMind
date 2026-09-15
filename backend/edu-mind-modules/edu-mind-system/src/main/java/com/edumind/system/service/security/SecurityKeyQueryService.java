package com.edumind.system.service.security;

public interface SecurityKeyQueryService {

    int getActiveKeyVersion(Long tenantId, String keyAlias);

    String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion);
}
