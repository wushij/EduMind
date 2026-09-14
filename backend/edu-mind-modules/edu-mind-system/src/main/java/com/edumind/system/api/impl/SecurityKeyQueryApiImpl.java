package com.edumind.system.api.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.system.api.SecurityKeyQueryApi;
import com.edumind.system.dao.security.SecurityKeyVersionDao;
import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * 国密 KMS 密钥跨模块门面实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityKeyQueryApiImpl implements SecurityKeyQueryApi {

    private final SecurityKeyVersionDao securityKeyVersionDao;

    @Value("${edumind.security.master-secret:${edumind.memory.key-secret:EduMind_Memory_Key_Seed_2026}}")
    private String masterSecret;

    @Value("${edumind.security.default-key-alias:edumind-data-key}")
    private String defaultKeyAlias;

    @Override
    public int getActiveKeyVersion(Long tenantId, String keyAlias) {
        Long tid = tenantId != null ? tenantId : TenantContext.getTenantId();
        if (tid == null) {
            tid = 1L;
        }
        String alias = StringUtils.hasText(keyAlias) ? keyAlias : defaultKeyAlias;
        SecurityKeyVersionEntity active = securityKeyVersionDao.findActiveByTenantAndAlias(tid, alias);
        if (active != null && active.getKeyVersion() != null) {
            return active.getKeyVersion();
        }
        // 懒初始化 v1
        SecurityKeyVersionEntity v1 = new SecurityKeyVersionEntity();
        v1.setTenantId(tid);
        v1.setKeyAlias(alias);
        v1.setKeyVersion(1);
        v1.setAlgorithm("SM4_GCM");
        v1.setStatus("ACTIVE");
        v1.setActivatedTime(LocalDateTime.now());
        try {
            securityKeyVersionDao.insert(v1);
            log.info("[KMS API] 租户 {} 别名 {} 懒初始化密钥版本 v1 成功", tid, alias);
        } catch (Exception e) {
            log.debug("[KMS API] 并发初始化或已有记录: {}", e.getMessage());
            SecurityKeyVersionEntity secondCheck = securityKeyVersionDao.findActiveByTenantAndAlias(tid, alias);
            if (secondCheck != null && secondCheck.getKeyVersion() != null) {
                return secondCheck.getKeyVersion();
            }
        }
        return 1;
    }

    @Override
    public String resolveDataKey16(Long tenantId, String keyAlias, int keyVersion) {
        Long tid = tenantId != null ? tenantId : 0L;
        try {
            String seed;
            boolean isCustomAlias = StringUtils.hasText(keyAlias) && !defaultKeyAlias.equals(keyAlias);
            String aliasPart = isCustomAlias ? "_" + keyAlias : "";
            if (keyVersion <= 1) {
                // 默认别名保持与旧长期记忆数据派生兼容
                seed = masterSecret + "_" + tid + aliasPart;
            } else {
                seed = masterSecret + "_" + tid + aliasPart + "_v" + keyVersion;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append((char) ('a' + (hash[i] & 0x0F)));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("[KMS API] 派生国密密钥材料失败", ex);
            return "EduMindMemKey16!";
        }
    }
}
