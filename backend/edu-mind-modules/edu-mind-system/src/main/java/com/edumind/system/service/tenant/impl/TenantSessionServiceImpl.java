package com.edumind.system.service.tenant.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.converter.TenantConverter;
import com.edumind.system.dao.SysCampusDao;
import com.edumind.system.dao.SysTenantDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dto.tenant.TenantSwitchDTO;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.service.SysTenantService;
import com.edumind.system.service.tenant.TenantSessionService;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TenantSessionServiceImpl implements TenantSessionService {

    private final SysTenantDao sysTenantDao;
    private final SysCampusDao sysCampusDao;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final TenantConverter tenantConverter;
    private final SysTenantService sysTenantService;

    public TenantSessionServiceImpl(SysTenantDao sysTenantDao,
                                      SysCampusDao sysCampusDao,
                                      SysTenantMemberDao sysTenantMemberDao,
                                      TenantConverter tenantConverter,
                                      @Lazy SysTenantService sysTenantService) {
        this.sysTenantDao = sysTenantDao;
        this.sysCampusDao = sysCampusDao;
        this.sysTenantMemberDao = sysTenantMemberDao;
        this.tenantConverter = tenantConverter;
        this.sysTenantService = sysTenantService;
    }

    @Value("${edumind.tenant.demo-auto-bind-enabled:true}")
    private boolean demoAutoBindEnabled = true;

    @Override
    public List<TenantListVO> listUserAvailableTenants(Long userId) {
        // 「可切换租户列表」本身需要跨租户视角，统一用 callWithoutTenant 管理上下文进入与恢复
        return TenantContext.callWithoutTenant(() -> {
            boolean isGlobalAdmin = Long.valueOf(1L).equals(userId)
                    || StpUtil.hasRole("ADMIN")
                    || StpUtil.hasRole("PLATFORM_ADMIN")
                    || StpUtil.hasRole("ROLE_ADMIN");

            if (isGlobalAdmin) {
                List<SysTenantEntity> allActive = sysTenantDao.listAllActive();
                return allActive.stream().map(t -> {
                    int campusCount = (int) sysCampusDao.countByTenantId(t.getId());
                    return tenantConverter.toListVO(t, Math.max(1, campusCount), 0);
                }).collect(Collectors.toList());
            }

            List<SysTenantMemberEntity> members = sysTenantMemberDao.listByUserId(userId);
            if (members.isEmpty()) {
                SysTenantEntity defaultTenant = sysTenantDao.findById(1L);
                if (defaultTenant != null) {
                    return List.of(tenantConverter.toListVO(defaultTenant, 1, 25));
                }
                return List.of();
            }
            List<Long> tenantIds = members.stream().map(SysTenantMemberEntity::getTenantId).distinct().toList();
            List<SysTenantEntity> tenants = sysTenantDao.listByIds(tenantIds);
            return tenants.stream().map(t -> {
                int campusCount = (int) sysCampusDao.countByTenantId(t.getId());
                return tenantConverter.toListVO(t, Math.max(1, campusCount), 30);
            }).collect(Collectors.toList());
        });
    }

    @Override
    public TenantDetailVO getCurrentTenantInfo() {
        return TenantContext.callWithoutTenant(() -> {
            Long tenantId = TenantContext.requireTenantId();
            return sysTenantService.getTenantDetail(tenantId);
        });
    }

    @Override
    public Long initializeLoginTenantSession(Long userId) {
        // 登录初始化需要在全量租户视图中判断成员关系与管理员身份
        return TenantContext.callWithoutTenant(() -> {
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
        });
    }

    @Override
    public Map<String, Object> switchTenant(TenantSwitchDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        SysTenantEntity targetTenant = sysTenantDao.findById(dto.getTargetTenantId());
        if (targetTenant == null || targetTenant.getStatus() != 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "目标租户不存在或已被停用");
        }

        boolean isMember = isUserMemberOfTenant(userId, dto.getTargetTenantId());
        boolean isDelegated = false;

        if (!isMember) {
            if (Long.valueOf(1L).equals(userId) || StpUtil.hasRole("ADMIN") || StpUtil.hasRole("PLATFORM_ADMIN") || StpUtil.hasRole("ROLE_ADMIN")) {
                isDelegated = true;
                log.info("[安全审计] 管理员 userId={} 申请代管进入租户 tenantId={}, 原因: {}",
                        userId, dto.getTargetTenantId(), dto.getReason());
            } else {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权进入该租户");
            }
        }

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

    private boolean isUserMemberOfTenant(Long userId, Long tenantId) {
        // 使用 callWithoutTenant 统一管理「忽略租户」的进入与恢复，避免裸调用漏写 finally 导致隔离被永久关闭
        return TenantContext.callWithoutTenant(() -> {
            SysTenantMemberEntity member = sysTenantMemberDao.findByTenantAndUser(tenantId, userId);
            return member != null && member.getStatus() == 1;
        });
    }
}
