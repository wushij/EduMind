package com.edumind.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.security.context.TenantContext;
import com.edumind.system.api.TenantQueryApi;
import com.edumind.system.converter.TenantConverter;
import com.edumind.system.dao.SysCampusDao;
import com.edumind.system.dao.SysTenantDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dao.SysTenantQuotaDao;
import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.entity.SysCampusEntity;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.service.SysTenantService;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService, TenantQueryApi {

    private final SysTenantDao sysTenantDao;
    private final SysCampusDao sysCampusDao;
    private final SysTenantQuotaDao sysTenantQuotaDao;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final TenantConverter tenantConverter;

    @org.springframework.beans.factory.annotation.Value("${edumind.tenant.demo-auto-bind-enabled:true}")
    private boolean demoAutoBindEnabled = true;

    @Override
    public Page<TenantListVO> pageTenants(int page, int pageSize, String keyword, Integer status) {
        Page<SysTenantEntity> entityPage = sysTenantDao.page(page, pageSize, keyword, status);
        List<TenantListVO> voList = entityPage.getRecords().stream().map(entity -> {
            List<SysCampusEntity> campuses = sysCampusDao.listByTenantId(entity.getId());
            SysTenantQuotaEntity tokenQuota = sysTenantQuotaDao.findByTenantAndType(entity.getId(), "TOKEN");
            long usagePercent = 0;
            if (tokenQuota != null && tokenQuota.getLimitValue() != null && tokenQuota.getLimitValue() > 0) {
                usagePercent = Math.min(100, Math.round((double) tokenQuota.getUsedValue() / tokenQuota.getLimitValue() * 100));
            }
            return tenantConverter.toListVO(entity, campuses.size(), usagePercent);
        }).collect(Collectors.toList());

        Page<TenantListVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public TenantDetailVO getTenantDetail(Long tenantId) {
        SysTenantEntity entity = sysTenantDao.findById(tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "学校租户不存在");
        }
        List<SysCampusEntity> campuses = sysCampusDao.listByTenantId(tenantId);
        List<SysTenantQuotaEntity> quotas = sysTenantQuotaDao.listByTenantId(tenantId);
        return tenantConverter.toDetailVO(entity, campuses, quotas);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTenant(TenantCreateDTO dto) {
        SysTenantEntity exist = sysTenantDao.findByCode(dto.getCode());
        if (exist != null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "学校编码已存在: " + dto.getCode());
        }
        SysTenantEntity entity = tenantConverter.toEntity(dto);
        sysTenantDao.insert(entity);

        // 初始化默认主校区
        SysCampusEntity campus = new SysCampusEntity();
        campus.setTenantId(entity.getId());
        campus.setCode("MAIN");
        campus.setName(entity.getName() + "(本部)");
        campus.setStatus(1);
        sysCampusDao.insert(campus);

        // 初始化基础配额
        initDefaultQuota(entity.getId(), "TOKEN", 20000000L);
        initDefaultQuota(entity.getId(), "STORAGE", 200L);
        initDefaultQuota(entity.getId(), "QPS", 100L);
        initDefaultQuota(entity.getId(), "SEATS", 1000L);

        return entity.getId();
    }

    private void initDefaultQuota(Long tenantId, String type, Long limit) {
        SysTenantQuotaEntity q = new SysTenantQuotaEntity();
        q.setTenantId(tenantId);
        q.setQuotaType(type);
        q.setLimitValue(limit);
        q.setUsedValue(0L);
        q.setWarningThreshold(85);
        sysTenantQuotaDao.insert(q);
    }

    @Override
    public List<TenantListVO> listUserAvailableTenants(Long userId) {
        List<SysTenantMemberEntity> members = sysTenantMemberDao.listByUserId(userId);
        if (members.isEmpty()) {
            // 保障性默认租户
            SysTenantEntity defaultTenant = sysTenantDao.findById(1L);
            if (defaultTenant != null) {
                return List.of(tenantConverter.toListVO(defaultTenant, 1, 25));
            }
            return List.of();
        }
        List<Long> tenantIds = members.stream().map(SysTenantMemberEntity::getTenantId).distinct().toList();
        List<SysTenantEntity> tenants = sysTenantDao.listByIds(tenantIds);
        return tenants.stream().map(t -> tenantConverter.toListVO(t, 1, 30)).collect(Collectors.toList());
    }

    @Override
    public TenantDetailVO getCurrentTenantInfo() {
        Long tenantId = TenantContext.requireTenantId();
        return getTenantDetail(tenantId);
    }

    @Override
    public Long initializeLoginTenantSession(Long userId) {
        List<SysTenantMemberEntity> members = sysTenantMemberDao.listByUserId(userId);
        Long tenantId = null;
        boolean isDelegated = false;

        if (!members.isEmpty()) {
            tenantId = members.stream()
                    .filter(m -> m.getIsDefault() != null && m.getIsDefault() == 1)
                    .map(SysTenantMemberEntity::getTenantId)
                    .findFirst()
                    .orElse(members.get(0).getTenantId());
        } else if (Long.valueOf(1L).equals(userId) || StpUtil.hasRole("ADMIN") || StpUtil.hasRole("PLATFORM_ADMIN") || StpUtil.hasRole("ROLE_ADMIN")) {
            SysTenantEntity defaultTenant = sysTenantDao.findById(1L);
            if (defaultTenant != null && defaultTenant.getStatus() == 1) {
                tenantId = 1L;
                isDelegated = true;
                log.info("[登录初始化] 管理员 userId={} 无租户成员关系，代管进入默认租户 tenantId=1", userId);
            }
        } else if (demoAutoBindEnabled) {
            // 普通用户若无租户绑定，且开启了 demo 自动绑定特性，自动绑定默认示范租户 1
            SysTenantEntity defaultTenant = sysTenantDao.findById(1L);
            if (defaultTenant != null && defaultTenant.getStatus() == 1) {
                tenantId = 1L;
                SysTenantMemberEntity newMember = new SysTenantMemberEntity();
                newMember.setTenantId(1L);
                newMember.setUserId(userId);
                newMember.setMemberNo("USER-" + userId);
                newMember.setRealName("学员用户");
                newMember.setStatus(1);
                newMember.setIsDefault(1);
                sysTenantMemberDao.insert(newMember);
                log.info("[登录初始化] 普通用户 userId={} 自动绑定默认租户 tenantId=1 (demo 自动绑定模式)", userId);
            }
        } else {
            // 正式多校环境：未加入任何学校的用户禁止登录，返回 null
            log.warn("[登录初始化] 用户 userId={} 未加入任何学校/租户且未启用 demo 自动绑定，拒绝登录", userId);
            tenantId = null;
        }

        if (tenantId != null) {
            StpUtil.getSession().set("tenantId", tenantId);
            StpUtil.getSession().set("isDelegated", isDelegated);
        } else {
            log.warn("[登录初始化] 用户 userId={} 未能绑定租户上下文", userId);
        }
        return tenantId;
    }

    @Override
    public Map<String, Object> switchTenant(TenantSwitchDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        SysTenantEntity targetTenant = sysTenantDao.findById(dto.getTargetTenantId());
        if (targetTenant == null || targetTenant.getStatus() != 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "目标租户不存在或已被停用");
        }

        // 成员关系或平台代管权限判定
        boolean isMember = isUserMemberOfTenant(userId, dto.getTargetTenantId());
        boolean isDelegated = false;

        if (!isMember) {
            // 检查当前用户是否为平台超级管理员 (具有全局管理权)
            if (Long.valueOf(1L).equals(userId) || StpUtil.hasRole("ADMIN") || StpUtil.hasRole("PLATFORM_ADMIN") || StpUtil.hasRole("ROLE_ADMIN")) {
                isDelegated = true;
                log.info("[安全审计] 管理员 userId={} 申请代管进入租户 tenantId={}, 原因: {}",
                        userId, dto.getTargetTenantId(), dto.getReason());
            } else {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权进入该租户");
            }
        }

        // 写入当前 Sa-Token 会话
        StpUtil.getSession().set("tenantId", dto.getTargetTenantId());
        StpUtil.getSession().set("isDelegated", isDelegated);

        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", targetTenant.getId());
        result.put("tenantName", targetTenant.getName());
        result.put("tenantCode", targetTenant.getCode());
        result.put("isDelegated", isDelegated);
        result.put("token", StpUtil.getTokenValue());
        return result;
    }

    // --- TenantQueryApi 跨模块实现 ---

    @Override
    public Map<String, Object> getTenantById(Long tenantId) {
        SysTenantEntity entity = sysTenantDao.findById(tenantId);
        if (entity == null) return Map.of();
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("code", entity.getCode());
        map.put("name", entity.getName());
        map.put("logo", entity.getLogo());
        map.put("status", entity.getStatus());
        return map;
    }

    @Override
    public Long getTenantIdByCode(String code) {
        SysTenantEntity entity = sysTenantDao.findByCode(code);
        return entity != null ? entity.getId() : null;
    }

    @Override
    public List<Map<String, Object>> listAvailableTenants(Long userId) {
        return listUserAvailableTenants(userId).stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("code", t.getCode());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isUserMemberOfTenant(Long userId, Long tenantId) {
        SysTenantMemberEntity member = sysTenantMemberDao.findByTenantAndUser(tenantId, userId);
        return member != null && member.getStatus() == 1;
    }

    @Override
    public List<Long> listActiveTenantIds() {
        return sysTenantDao.listAllActive().stream()
                .map(SysTenantEntity::getId)
                .collect(Collectors.toList());
    }
}
