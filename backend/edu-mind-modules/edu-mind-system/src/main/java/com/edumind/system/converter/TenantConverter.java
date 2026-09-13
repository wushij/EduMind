package com.edumind.system.converter;

import com.edumind.system.dto.tenant.TenantCreateDTO;
import com.edumind.system.entity.SysCampusEntity;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.vo.tenant.CampusVO;
import com.edumind.system.vo.tenant.TenantDetailVO;
import com.edumind.system.vo.tenant.TenantListVO;
import com.edumind.system.vo.tenant.TenantQuotaVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TenantConverter {

    public TenantListVO toListVO(SysTenantEntity entity, int campusCount, long tokenUsagePercent) {
        if (entity == null) return null;
        TenantListVO vo = new TenantListVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setLogo(entity.getLogo());
        vo.setDomain(entity.getDomain());
        vo.setPlanCode(entity.getPlanCode());
        vo.setStatus(entity.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCampusCount(campusCount);
        vo.setTokenUsagePercent(tokenUsagePercent);
        return vo;
    }

    public TenantDetailVO toDetailVO(SysTenantEntity entity, List<SysCampusEntity> campuses, List<SysTenantQuotaEntity> quotas) {
        if (entity == null) return null;
        TenantDetailVO vo = new TenantDetailVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setLogo(entity.getLogo());
        vo.setDomain(entity.getDomain());
        vo.setPlanCode(entity.getPlanCode());
        vo.setStatus(entity.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setCreateTime(entity.getCreateTime());

        if (campuses != null) {
            vo.setCampuses(campuses.stream().map(this::toCampusVO).collect(Collectors.toList()));
        }
        if (quotas != null) {
            vo.setQuotas(quotas.stream().map(this::toQuotaVO).collect(Collectors.toList()));
        }
        return vo;
    }

    public CampusVO toCampusVO(SysCampusEntity entity) {
        if (entity == null) return null;
        CampusVO vo = new CampusVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setAddress(entity.getAddress());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    public TenantQuotaVO toQuotaVO(SysTenantQuotaEntity entity) {
        if (entity == null) return null;
        TenantQuotaVO vo = new TenantQuotaVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setQuotaType(entity.getQuotaType());
        vo.setLimitValue(entity.getLimitValue());
        vo.setUsedValue(entity.getUsedValue());
        int percent = entity.getLimitValue() != null && entity.getLimitValue() > 0
                ? (int) Math.min(100, Math.round((double) entity.getUsedValue() / entity.getLimitValue() * 100))
                : 0;
        vo.setUsagePercent(percent);
        vo.setWarningThreshold(entity.getWarningThreshold() != null ? entity.getWarningThreshold() : 85);
        vo.setWarning(percent >= vo.getWarningThreshold());
        return vo;
    }

    public SysTenantEntity toEntity(TenantCreateDTO dto) {
        if (dto == null) return null;
        SysTenantEntity entity = new SysTenantEntity();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setLogo(dto.getLogo() != null ? dto.getLogo() : "/assets/logo.png");
        entity.setDomain(dto.getDomain());
        entity.setPlanCode(dto.getPlanCode() != null ? dto.getPlanCode() : "STANDARD");
        entity.setStatus(1);
        entity.setExpireTime(dto.getExpireTime());
        return entity;
    }
}
