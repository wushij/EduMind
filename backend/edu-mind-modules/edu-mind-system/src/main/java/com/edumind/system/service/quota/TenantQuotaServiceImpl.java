package com.edumind.system.service.quota;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.converter.TenantConverter;
import com.edumind.system.dao.SysTenantQuotaDao;
import com.edumind.system.dto.tenant.QuotaUpdateDTO;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.vo.tenant.TenantQuotaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户配额管理与原子扣减服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQuotaServiceImpl implements TenantQuotaService {

    private final SysTenantQuotaDao sysTenantQuotaDao;
    private final TenantConverter tenantConverter;
    private final UserQueryApi userQueryApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean consumeTokenQuota(Long tenantId, long tokenDelta) {
        if (tenantId == null || tokenDelta <= 0) {
            return true;
        }

        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(tenantId, "TOKEN");
            if (entity == null) {
                // 若尚无配额记录，初始化默认 20,000,000 Token 上限
                entity = new SysTenantQuotaEntity();
                entity.setTenantId(tenantId);
                entity.setQuotaType("TOKEN");
                entity.setLimitValue(20000000L);
                entity.setUsedValue(0L);
                entity.setWarningThreshold(85);
                entity.setUpdateTime(LocalDateTime.now());
                sysTenantQuotaDao.insert(entity);
            }

            int updated = sysTenantQuotaDao.consumeQuotaAtomic(tenantId, "TOKEN", tokenDelta);
            if (updated > 0) {
                SysTenantQuotaEntity latest = sysTenantQuotaDao.findByTenantAndType(tenantId, "TOKEN");
                long currentUsed = latest != null && latest.getUsedValue() != null ? latest.getUsedValue() : (entity.getUsedValue() != null ? entity.getUsedValue() : 0L) + tokenDelta;
                long limit = latest != null && latest.getLimitValue() != null ? latest.getLimitValue() : (entity.getLimitValue() != null ? entity.getLimitValue() : 20000000L);
                int warningThreshold = latest != null && latest.getWarningThreshold() != null ? latest.getWarningThreshold() : 85;

                if (limit > 0) {
                    double percent = ((double) currentUsed / limit) * 100;
                    if (percent >= warningThreshold) {
                        log.warn("⚠️ [租户配额预警] 租户 {} 的 TOKEN 配额消耗已达 {}% (已用: {} / 上限: {})",
                                tenantId, String.format("%.1f", percent), currentUsed, limit);
                    }
                }
                return true;
            }

            // 扣减失败说明已超出限制上限
            log.error("❌ [租户配额超限] 租户 {} 的 TOKEN 配额不足，扣减增量: {}, 已用: {}, 上限: {}",
                    tenantId, tokenDelta, entity.getUsedValue(), entity.getLimitValue());
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "租户 Token 配额已耗尽，请联系管理员扩容");
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public void checkTokenQuotaAvailable(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return;
        }
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(tenantId, "TOKEN");
            if (entity != null && entity.getLimitValue() != null && entity.getUsedValue() != null) {
                if (entity.getUsedValue() >= entity.getLimitValue()) {
                    log.warn("[租户配额预检阻断] 租户 {} 的 TOKEN 配额已耗尽 ({} / {})",
                            tenantId, entity.getUsedValue(), entity.getLimitValue());
                    throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "租户 Token 配额已耗尽，请联系管理员扩容");
                }
            }
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public TenantQuotaVO getTokenQuotaStatus(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(tenantId, "TOKEN");
            return entity != null ? tenantConverter.toQuotaVO(entity) : null;
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public List<TenantQuotaVO> listQuotas(Long requestTenantId) {
        Long resolvedTenantId = resolveSecureTenantId(requestTenantId);
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(resolvedTenantId);
            List<SysTenantQuotaEntity> list = sysTenantQuotaDao.listByTenantId(resolvedTenantId);
            return list.stream().map(tenantConverter::toQuotaVO).collect(Collectors.toList());
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotaThreshold(QuotaUpdateDTO dto) {
        if (dto == null || dto.getQuotaType() == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "配额类型不能为空");
        }
        Long resolvedTenantId = resolveSecureTenantId(dto.getTenantId());
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(resolvedTenantId);
            SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(resolvedTenantId, dto.getQuotaType());
            if (entity != null) {
                if (dto.getWarningThreshold() != null) {
                    entity.setWarningThreshold(dto.getWarningThreshold());
                }
                if (dto.getLimitValue() != null) {
                    entity.setLimitValue(dto.getLimitValue());
                }
                entity.setUpdateTime(LocalDateTime.now());
                sysTenantQuotaDao.updateById(entity);
            } else {
                entity = new SysTenantQuotaEntity();
                entity.setTenantId(resolvedTenantId);
                entity.setQuotaType(dto.getQuotaType());
                entity.setLimitValue(dto.getLimitValue() != null ? dto.getLimitValue() : 10000000L);
                entity.setUsedValue(0L);
                entity.setWarningThreshold(dto.getWarningThreshold() != null ? dto.getWarningThreshold() : 85);
                entity.setUpdateTime(LocalDateTime.now());
                sysTenantQuotaDao.insert(entity);
            }
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * 防范 IDOR 越权与策略文档化：
     * 1. 严格以当前登录用户的租户上下文为准 (TenantContext.requireTenantId())；
     * 2. 若当前为平台超级管理员 (ADMIN / PLATFORM_ADMIN)，允许跨租户指定 requestedTenantId；
     * 3. 若为普通用户或校级管理员 (TENANT_ADMIN)，跨租户非法请求将被静默改写为当前租户 (Demo 友好容错策略)，杜绝越权访问其他学校配额。
     *
     * @param requestedTenantId 外部请求传入的租户ID（可空）
     * @return 安全判定后的租户ID
     */
    private Long resolveSecureTenantId(Long requestedTenantId) {
        Long currentTenantId = TenantContext.requireTenantId();
        if (requestedTenantId == null || requestedTenantId.equals(currentTenantId)) {
            return currentTenantId;
        }
        // 校验当前用户是否为平台超级管理员
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<String> roles = userQueryApi.getRolesByUserId(userId);
            if (roles != null && (roles.contains(RoleCode.ADMIN.getCode()) || roles.contains("PLATFORM_ADMIN"))) {
                return requestedTenantId;
            }
        }
        // 普通用户试图查看/修改其他租户配额，一律阻断并重写为当前租户
        log.warn("[防越权] 拦截非超管跨租户访问配额请求: currentTenantId={}, requestedTenantId={}",
                currentTenantId, requestedTenantId);
        return currentTenantId;
    }
}
