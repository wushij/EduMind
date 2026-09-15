package com.edumind.system.service.query.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.system.dao.SysTenantDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.service.query.TenantQueryService;
import com.edumind.system.service.tenant.TenantSessionService;
import com.edumind.system.vo.tenant.TenantBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantQueryServiceImpl implements TenantQueryService {

    private final SysTenantDao sysTenantDao;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final TenantSessionService tenantSessionService;

    @Override
    public TenantBriefVO getTenantById(Long tenantId) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            SysTenantEntity entity = sysTenantDao.findById(tenantId);
            if (entity == null) {
                return new TenantBriefVO();
            }
            return TenantBriefVO.builder()
                    .id(entity.getId())
                    .code(entity.getCode())
                    .name(entity.getName())
                    .logo(entity.getLogo())
                    .status(entity.getStatus())
                    .build();
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    public Long getTenantIdByCode(String code) {
        SysTenantEntity entity = sysTenantDao.findByCode(code);
        return entity != null ? entity.getId() : null;
    }

    @Override
    public List<TenantBriefVO> listAvailableTenants(Long userId) {
        return tenantSessionService.listUserAvailableTenants(userId).stream().map(t -> TenantBriefVO.builder()
                .id(t.getId())
                .name(t.getName())
                .code(t.getCode())
                .build()).collect(Collectors.toList());
    }

    @Override
    public boolean isUserMemberOfTenant(Long userId, Long tenantId) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            SysTenantMemberEntity member = sysTenantMemberDao.findByTenantAndUser(tenantId, userId);
            return member != null && member.getStatus() == 1;
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    public List<Long> listActiveTenantIds() {
        return sysTenantDao.listAllActive().stream()
                .map(SysTenantEntity::getId)
                .collect(Collectors.toList());
    }
}
