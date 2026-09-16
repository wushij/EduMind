package com.edumind.system.api;

import com.edumind.system.vo.tenant.TenantQuotaVO;

/**
 * 租户配额领域跨模块服务 API
 */
public interface TenantQuotaApi {

    /**
     * 原子扣减租户 TOKEN 配额；成功返回 true，超额抛出 429 业务异常
     *
     * @param tenantId   租户 ID
     * @param tokenDelta 需扣减的 Token 增量
     * @return true 扣减成功
     */
    boolean consumeTokenQuota(Long tenantId, long tokenDelta);

    /**
     * 校验租户 Token 配额是否仍有可用余量 (预检防刷)
     *
     * @param tenantId 租户 ID
     */
    void checkTokenQuotaAvailable(Long tenantId);

    /**
     * 获取租户当前 Token 配额状态 (用量、上限、百分比、预警标志)
     *
     * @param tenantId 租户 ID
     * @return 配额状态 VO
     */
    TenantQuotaVO getTokenQuotaStatus(Long tenantId);
}
