package com.edumind.system.api;

/**
 * 国密 KMS 密钥查询与解析公开 API（跨模块安全门面）
 */
public interface SecurityKeyQueryApi {

    /**
     * 获取租户指定密钥别名的当前 ACTIVE 密钥版本号；若不存在则懒初始化 v1 并返回 1
     *
     * @param tenantId 租户 ID
     * @param keyAlias 密钥别名（如 edumind-data-key）
     * @return 当前有效版本号 (>= 1)
     */
    int getActiveKeyVersion(Long tenantId, String keyAlias);

    /**
     * 内存派生 16 字节 SM4 密钥材料（用于 SM4-GCM 加解密）
     * 严格禁止落盘，禁止输出到日志
     *
     * @param tenantId 租户 ID
     * @param keyAlias 密钥别名
     * @param keyVersion 密钥版本号
     * @return 16 字符 SM4 密钥字符串
     */
    String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion);
}
