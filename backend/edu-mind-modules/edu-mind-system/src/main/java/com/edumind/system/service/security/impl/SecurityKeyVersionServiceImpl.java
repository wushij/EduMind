package com.edumind.system.service.security.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.converter.security.SecurityKeyVersionConverter;
import com.edumind.system.dao.security.SecurityKeyVersionDao;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.service.security.SecurityKeyVersionService;
import com.edumind.system.vo.security.SecurityKeyVersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 国密密钥版本管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityKeyVersionServiceImpl implements SecurityKeyVersionService {

    private final SecurityKeyVersionDao securityKeyVersionDao;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String defaultKeyAlias;

    @Override
    public List<SecurityKeyVersionVO> listKeyVersions(String keyAlias) {
        Long tenantId = TenantContext.requireTenantId();
        String alias = StringUtils.hasText(keyAlias) ? keyAlias : defaultKeyAlias;
        List<SecurityKeyVersionEntity> list = securityKeyVersionDao.listByTenantAndAlias(tenantId, alias);

        // 若当前租户下暂无任何版本，懒初始化 v1 ACTIVE（保证开箱即用）
        if (list.isEmpty()) {
            ensureLazyInitActiveKey(tenantId, alias);
            list = securityKeyVersionDao.listByTenantAndAlias(tenantId, alias);
        }

        return SecurityKeyVersionConverter.toVOList(list);
    }

    @Override
    public SecurityKeyVersionVO getActiveKey(String keyAlias) {
        Long tenantId = TenantContext.requireTenantId();
        String alias = StringUtils.hasText(keyAlias) ? keyAlias : defaultKeyAlias;
        SecurityKeyVersionEntity active = securityKeyVersionDao.findActiveByTenantAndAlias(tenantId, alias);
        if (active == null) {
            active = ensureLazyInitActiveKey(tenantId, alias);
        }
        return SecurityKeyVersionConverter.toVO(active);
    }

    @Override
    public SecurityKeyVersionVO getKeyVersion(Long id) {
        SecurityKeyVersionEntity entity = requireAccessibleKey(id);
        return SecurityKeyVersionConverter.toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SecurityKeyVersionVO rotateKey(SecurityKeyRotateDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "轮换请求参数不能为空");
        }

        // 若传入了目标 ID，优先执行 IDOR 归属权限校验
        if (dto.getId() != null) {
            requireAccessibleKey(dto.getId());
        }

        Long tenantId = TenantContext.requireTenantId();
        String alias = StringUtils.hasText(dto.getKeyAlias()) ? dto.getKeyAlias() : defaultKeyAlias;

        // 1. 获取当前 ACTIVE 记录
        SecurityKeyVersionEntity active = securityKeyVersionDao.findActiveByTenantAndAlias(tenantId, alias);
        if (active == null) {
            active = ensureLazyInitActiveKey(tenantId, alias);
        }

        // 2. 将旧活跃版本标记为 DEPRECATED
        active.setStatus("DEPRECATED");
        securityKeyVersionDao.updateById(active);

        // 3. 计算递增版本号并插入全新 ACTIVE 记录
        int maxVer = securityKeyVersionDao.findMaxVersion(tenantId, alias);
        int nextVer = Math.max(maxVer, active.getKeyVersion()) + 1;

        SecurityKeyVersionEntity newEntity = new SecurityKeyVersionEntity();
        newEntity.setTenantId(tenantId);
        newEntity.setKeyAlias(alias);
        newEntity.setKeyVersion(nextVer);
        newEntity.setAlgorithm("SM4_GCM");
        newEntity.setStatus("ACTIVE");
        newEntity.setActivatedTime(LocalDateTime.now());
        securityKeyVersionDao.insert(newEntity);

        log.info("[国密KMS密钥轮换成功] 租户: {}, 别名: {}, 旧版本: v{} -> 新版本: v{}",
                tenantId, alias, active.getKeyVersion(), nextVer);

        return SecurityKeyVersionConverter.toVO(newEntity);
    }

    private SecurityKeyVersionEntity requireAccessibleKey(Long id) {
        SecurityKeyVersionEntity entity = securityKeyVersionDao.findByIdIgnoreTenant(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "密钥版本记录不存在");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作其他租户的国密密钥资产 (IDOR 越权拦截)");
        }
        return entity;
    }

    private SecurityKeyVersionEntity ensureLazyInitActiveKey(Long tenantId, String alias) {
        SecurityKeyVersionEntity active = securityKeyVersionDao.findActiveByTenantAndAlias(tenantId, alias);
        if (active != null) {
            return active;
        }
        SecurityKeyVersionEntity v1 = new SecurityKeyVersionEntity();
        v1.setTenantId(tenantId);
        v1.setKeyAlias(alias);
        v1.setKeyVersion(1);
        v1.setAlgorithm("SM4_GCM");
        v1.setStatus("ACTIVE");
        v1.setActivatedTime(LocalDateTime.now());
        securityKeyVersionDao.insert(v1);
        log.info("[国密KMS懒初始化] 租户: {}, 别名: {}, 初始生效版本: v1", tenantId, alias);
        return v1;
    }
}
