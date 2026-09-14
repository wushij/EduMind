package com.edumind.system.service.security.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.security.crypto.Sm4Service;
import com.edumind.system.api.SecurityKeyQueryApi;
import com.edumind.system.converter.security.SecurityKeyVersionConverter;
import com.edumind.system.dao.security.SecurityKeyVersionDao;
import com.edumind.system.dto.security.SecurityKeyCryptoTestDTO;
import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.service.security.SecurityKeyVersionService;
import com.edumind.system.vo.security.SecurityKeyCryptoTestVO;
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
    private final SecurityKeyQueryApi securityKeyQueryApi;
    private final Sm4Service sm4Service;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String defaultKeyAlias;

    @Override
    public List<SecurityKeyVersionVO> listKeyVersions(String keyAlias) {
        Long tenantId = TenantContext.requireTenantId();
        // 保证租户数据加密主密钥与AI大模型凭证密钥开箱即用生效
        ensureLazyInitActiveKey(tenantId, defaultKeyAlias);
        ensureLazyInitActiveKey(tenantId, "edumind-model-key");

        String queryAlias = StringUtils.hasText(keyAlias) ? keyAlias.trim() : null;
        List<SecurityKeyVersionEntity> list = securityKeyVersionDao.listByTenantAndAlias(tenantId, queryAlias);
        return SecurityKeyVersionConverter.toVOList(list);
    }

    @Override
    public SecurityKeyVersionVO getActiveKey(String keyAlias) {
        Long tenantId = TenantContext.requireTenantId();
        String alias = StringUtils.hasText(keyAlias) ? keyAlias.trim() : defaultKeyAlias;
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

    @Override
    public SecurityKeyCryptoTestVO testCrypto(SecurityKeyCryptoTestDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getText())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "测试输入内容不能为空");
        }
        Long tenantId = TenantContext.requireTenantId();
        String alias = StringUtils.hasText(dto.getKeyAlias()) ? dto.getKeyAlias().trim() : defaultKeyAlias;
        int version = dto.getKeyVersion() != null && dto.getKeyVersion() > 0
                ? dto.getKeyVersion()
                : securityKeyQueryApi.getActiveKeyVersion(tenantId, alias);

        String key16 = securityKeyQueryApi.resolveDataKey16(tenantId, alias, version);

        SecurityKeyCryptoTestVO vo = new SecurityKeyCryptoTestVO();
        vo.setKeyAlias(alias);
        vo.setKeyVersion(version);
        vo.setAlgorithm("SM4-GCM");
        vo.setOperation(dto.getOperation());

        long start = System.currentTimeMillis();
        try {
            if ("DECRYPT".equalsIgnoreCase(dto.getOperation())) {
                String decrypted = sm4Service.decryptFromBase64(key16, dto.getText().trim());
                vo.setResultText(decrypted);
                vo.setSuccess(true);
                vo.setMessage("解密成功！密文真实由当前 KMS 密钥 (v" + version + ") 认证通过并还原明文");
            } else {
                String encrypted = sm4Service.encryptToBase64(key16, dto.getText().trim());
                vo.setResultText(encrypted);
                vo.setSuccess(true);
                vo.setMessage("加密成功！已生成标准国密 SM4-GCM Base64 认证密文");
            }
        } catch (Exception e) {
            log.warn("[国密自检异常] 操作: {}, 租户: {}, 别名: {}, 版本: v{}, 原因: {}",
                    dto.getOperation(), tenantId, alias, version, e.getMessage());
            vo.setSuccess(false);
            vo.setResultText(null);
            vo.setMessage("执行失败: " + e.getMessage() + " (Fail-Closed 保护已生效，密文认证未通过或格式异常)");
        }
        vo.setDurationMs(System.currentTimeMillis() - start);
        return vo;
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
