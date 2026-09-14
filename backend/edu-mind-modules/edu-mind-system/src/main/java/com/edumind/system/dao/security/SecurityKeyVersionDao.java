package com.edumind.system.dao.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.mapper.security.SecurityKeyVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 国密密钥版本数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class SecurityKeyVersionDao {

    private final SecurityKeyVersionMapper securityKeyVersionMapper;

    /**
     * 按 ID 加载密钥版本（忽略租户 SQL 拦截），仅用于 IDOR 归属校验
     */
    public SecurityKeyVersionEntity findByIdIgnoreTenant(Long id) {
        if (id == null) {
            return null;
        }
        final SecurityKeyVersionEntity[] holder = new SecurityKeyVersionEntity[1];
        TenantContext.runWithoutTenant(() -> holder[0] = securityKeyVersionMapper.selectById(id));
        return holder[0];
    }

    /**
     * 查询指定租户与别名下的 ACTIVE 密钥版本（按版本降序取最新 1 条）
     */
    public SecurityKeyVersionEntity findActiveByTenantAndAlias(Long tenantId, String keyAlias) {
        final SecurityKeyVersionEntity[] holder = new SecurityKeyVersionEntity[1];
        TenantContext.runWithoutTenant(() -> holder[0] = securityKeyVersionMapper.selectOne(
                new LambdaQueryWrapper<SecurityKeyVersionEntity>()
                        .eq(SecurityKeyVersionEntity::getTenantId, tenantId)
                        .eq(SecurityKeyVersionEntity::getKeyAlias, keyAlias)
                        .eq(SecurityKeyVersionEntity::getStatus, "ACTIVE")
                        .orderByDesc(SecurityKeyVersionEntity::getKeyVersion)
                        .last("LIMIT 1")
        ));
        return holder[0];
    }

    /**
     * 查询指定租户与别名下的全量版本列表
     */
    public List<SecurityKeyVersionEntity> listByTenantAndAlias(Long tenantId, String keyAlias) {
        LambdaQueryWrapper<SecurityKeyVersionEntity> wrapper = new LambdaQueryWrapper<SecurityKeyVersionEntity>()
                .eq(SecurityKeyVersionEntity::getTenantId, tenantId);
        if (keyAlias != null && !keyAlias.isBlank()) {
            wrapper.eq(SecurityKeyVersionEntity::getKeyAlias, keyAlias);
        }
        wrapper.orderByDesc(SecurityKeyVersionEntity::getKeyVersion);
        return securityKeyVersionMapper.selectList(wrapper);
    }

    /**
     * 查询指定租户与别名下的最大版本号
     */
    public Integer findMaxVersion(Long tenantId, String keyAlias) {
        final SecurityKeyVersionEntity[] holder = new SecurityKeyVersionEntity[1];
        TenantContext.runWithoutTenant(() -> holder[0] = securityKeyVersionMapper.selectOne(
                new LambdaQueryWrapper<SecurityKeyVersionEntity>()
                        .eq(SecurityKeyVersionEntity::getTenantId, tenantId)
                        .eq(SecurityKeyVersionEntity::getKeyAlias, keyAlias)
                        .orderByDesc(SecurityKeyVersionEntity::getKeyVersion)
                        .last("LIMIT 1")
        ));
        return holder[0] != null && holder[0].getKeyVersion() != null ? holder[0].getKeyVersion() : 0;
    }

    public int insert(SecurityKeyVersionEntity entity) {
        return securityKeyVersionMapper.insert(entity);
    }

    public int updateById(SecurityKeyVersionEntity entity) {
        return securityKeyVersionMapper.updateById(entity);
    }
}
