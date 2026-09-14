package com.edumind.system.converter;

import com.edumind.system.entity.SysOrgQuotaEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.vo.tenant.OrgQuotaVO;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 组织院系算力配额 Converter
 */
@Component
public class OrgQuotaConverter {

    public OrgQuotaVO toVO(SysOrganizationEntity org,
                           String campusName,
                           Map<String, SysOrgQuotaEntity> quotaMap) {
        if (org == null) {
            return null;
        }
        OrgQuotaVO vo = new OrgQuotaVO();
        vo.setOrgId(org.getId());
        vo.setName(org.getName());
        vo.setOrgType(org.getOrgType());
        vo.setOrgTypeLabel(resolveOrgTypeLabel(org.getOrgType()));
        vo.setCampusName(campusName != null ? campusName : "主校区");

        SysOrgQuotaEntity tokenQuota = quotaMap != null ? quotaMap.get("TOKEN") : null;
        SysOrgQuotaEntity storageQuota = quotaMap != null ? quotaMap.get("STORAGE") : null;
        SysOrgQuotaEntity seatsQuota = quotaMap != null ? quotaMap.get("SEATS") : null;

        long tokenLimit = tokenQuota != null && tokenQuota.getLimitValue() != null ? tokenQuota.getLimitValue() : 10000000L;
        long tokenUsed = tokenQuota != null && tokenQuota.getUsedValue() != null ? tokenQuota.getUsedValue() : 0L;
        int warningThreshold = tokenQuota != null && tokenQuota.getWarningThreshold() != null ? tokenQuota.getWarningThreshold() : 85;

        vo.setTokenLimit(tokenLimit);
        vo.setTokenUsed(tokenUsed);
        vo.setWarningThreshold(warningThreshold);
        int percent = tokenLimit > 0 ? (int) Math.round(((double) tokenUsed / tokenLimit) * 100) : 0;
        vo.setUsagePercent(percent);

        if (percent >= 100) {
            vo.setStatus("EXCEEDED");
        } else if (percent >= warningThreshold) {
            vo.setStatus("WARNING");
        } else {
            vo.setStatus("NORMAL");
        }

        vo.setStorageLimit(storageQuota != null && storageQuota.getLimitValue() != null ? storageQuota.getLimitValue() : 100L);
        vo.setStorageUsed(storageQuota != null && storageQuota.getUsedValue() != null ? storageQuota.getUsedValue() : 0L);

        vo.setSeatsLimit(seatsQuota != null && seatsQuota.getLimitValue() != null ? seatsQuota.getLimitValue() : 300L);
        vo.setSeatsUsed(seatsQuota != null && seatsQuota.getUsedValue() != null ? seatsQuota.getUsedValue() : 0L);

        return vo;
    }

    public String resolveOrgTypeLabel(String orgType) {
        if (orgType == null) return "部门";
        return switch (orgType.toUpperCase()) {
            case "CAMPUS" -> "校区";
            case "COLLEGE", "FACULTY" -> "学院/大系";
            case "DEPT" -> "教研室/年级组";
            case "CLASS" -> "班级";
            default -> orgType;
        };
    }
}
