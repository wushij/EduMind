package com.edumind.system.service.quota;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.converter.OrgQuotaConverter;
import com.edumind.system.dao.SysCampusDao;
import com.edumind.system.dao.SysOrgQuotaDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dto.tenant.OrgQuotaUpdateDTO;
import com.edumind.system.entity.SysCampusEntity;
import com.edumind.system.entity.SysOrgQuotaEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.vo.tenant.OrgQuotaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织院系算力配额服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgQuotaServiceImpl implements OrgQuotaService {

    private final SysOrgQuotaDao sysOrgQuotaDao;
    private final SysOrganizationDao sysOrganizationDao;
    private final SysCampusDao sysCampusDao;
    private final OrgQuotaConverter orgQuotaConverter;
    private final UserQueryApi userQueryApi;

    @Override
    public List<OrgQuotaVO> listOrgQuotas(Long requestTenantId) {
        Long resolvedTenantId = resolveSecureTenantId(requestTenantId);
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(resolvedTenantId);

            // 1. 获取当前租户下所有组织机构
            List<SysOrganizationEntity> orgList = sysOrganizationDao.listByTenantId(resolvedTenantId);
            if (orgList == null || orgList.isEmpty()) {
                return Collections.emptyList();
            }

            // 过滤顶层校区节点，保留学院/教研组/年级/班级
            List<SysOrganizationEntity> targetOrgs = orgList.stream()
                    .filter(org -> !"CAMPUS".equalsIgnoreCase(org.getOrgType()))
                    .collect(Collectors.toList());

            // 2. 获取当前租户校区映射
            List<SysCampusEntity> campuses = sysCampusDao.listByTenantId(resolvedTenantId);
            Map<Long, String> campusMap = campuses.stream()
                    .collect(Collectors.toMap(SysCampusEntity::getId, SysCampusEntity::getName, (a, b) -> a));

            // 默认主校区名称
            String defaultCampusName = campuses.isEmpty() ? "主校区" : campuses.get(0).getName();

            // 3. 查询当前租户所有已配置的组织配额
            List<SysOrgQuotaEntity> quotaEntities = sysOrgQuotaDao.listByTenantId(resolvedTenantId);
            Map<Long, Map<String, SysOrgQuotaEntity>> orgQuotaMap = quotaEntities.stream()
                    .collect(Collectors.groupingBy(
                            SysOrgQuotaEntity::getOrgId,
                            Collectors.toMap(SysOrgQuotaEntity::getQuotaType, q -> q, (a, b) -> a)
                    ));

            // 4. 组装 VO，如果某些组织尚无持久化配额，自动按组织级别生成并持久化
            List<OrgQuotaVO> result = new ArrayList<>();
            for (SysOrganizationEntity org : targetOrgs) {
                Map<String, SysOrgQuotaEntity> typeMap = orgQuotaMap.get(org.getId());
                if (typeMap == null) {
                    typeMap = initDefaultOrgQuotas(resolvedTenantId, org);
                }
                OrgQuotaVO vo = orgQuotaConverter.toVO(org, defaultCampusName, typeMap);
                result.add(vo);
            }

            return result;
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
    public void updateOrgQuota(OrgQuotaUpdateDTO dto) {
        if (dto == null || dto.getOrgId() == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "组织节点ID不能为空");
        }
        Long resolvedTenantId = resolveSecureTenantId(dto.getTenantId());
        Long prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(resolvedTenantId);

            // 更新或创建 TOKEN 配额
            if (dto.getTokenLimit() != null) {
                saveOrUpdateTypeQuota(resolvedTenantId, dto.getOrgId(), "TOKEN",
                        dto.getTokenLimit(), dto.getWarningThreshold());
            }

            // 更新或创建 STORAGE 配额
            if (dto.getStorageLimit() != null) {
                saveOrUpdateTypeQuota(resolvedTenantId, dto.getOrgId(), "STORAGE",
                        dto.getStorageLimit(), dto.getWarningThreshold());
            }

            // 更新或创建 SEATS 配额
            if (dto.getSeatsLimit() != null) {
                saveOrUpdateTypeQuota(resolvedTenantId, dto.getOrgId(), "SEATS",
                        dto.getSeatsLimit(), dto.getWarningThreshold());
            }

            log.info("✅ [组织算力配额更新] 租户: {}, 组织ID: {}, Token上限: {}, 存储上限: {}, 席位上限: {}",
                    resolvedTenantId, dto.getOrgId(), dto.getTokenLimit(), dto.getStorageLimit(), dto.getSeatsLimit());
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private void saveOrUpdateTypeQuota(Long tenantId, Long orgId, String type, Long limitValue, Integer warningThreshold) {
        SysOrgQuotaEntity entity = sysOrgQuotaDao.findByTenantOrgAndType(tenantId, orgId, type);
        if (entity != null) {
            entity.setLimitValue(limitValue);
            if (warningThreshold != null) {
                entity.setWarningThreshold(warningThreshold);
            }
            entity.setUpdateTime(LocalDateTime.now());
            sysOrgQuotaDao.updateById(entity);
        } else {
            entity = new SysOrgQuotaEntity();
            entity.setTenantId(tenantId);
            entity.setOrgId(orgId);
            entity.setQuotaType(type);
            entity.setLimitValue(limitValue);
            entity.setUsedValue(0L);
            entity.setWarningThreshold(warningThreshold != null ? warningThreshold : 85);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            sysOrgQuotaDao.insert(entity);
        }
    }

    private Map<String, SysOrgQuotaEntity> initDefaultOrgQuotas(Long tenantId, SysOrganizationEntity org) {
        Map<String, SysOrgQuotaEntity> map = new HashMap<>();
        boolean isCollege = "COLLEGE".equalsIgnoreCase(org.getOrgType()) || "FACULTY".equalsIgnoreCase(org.getOrgType());

        long tokenLimit = isCollege ? 15000000L : 5000000L;
        long storageLimit = isCollege ? 150L : 50L;
        long seatsLimit = isCollege ? 600L : 200L;

        map.put("TOKEN", createAndInsertQuota(tenantId, org.getId(), "TOKEN", tokenLimit, 0L));
        map.put("STORAGE", createAndInsertQuota(tenantId, org.getId(), "STORAGE", storageLimit, 0L));
        map.put("SEATS", createAndInsertQuota(tenantId, org.getId(), "SEATS", seatsLimit, 0L));
        return map;
    }

    private SysOrgQuotaEntity createAndInsertQuota(Long tenantId, Long orgId, String quotaType, long limit, long used) {
        SysOrgQuotaEntity q = new SysOrgQuotaEntity();
        q.setTenantId(tenantId);
        q.setOrgId(orgId);
        q.setQuotaType(quotaType);
        q.setLimitValue(limit);
        q.setUsedValue(used);
        q.setWarningThreshold(85);
        q.setCreateTime(LocalDateTime.now());
        q.setUpdateTime(LocalDateTime.now());
        sysOrgQuotaDao.insert(q);
        return q;
    }

    private Long resolveSecureTenantId(Long requestedTenantId) {
        Long currentTenantId = TenantContext.requireTenantId();
        if (requestedTenantId == null || requestedTenantId.equals(currentTenantId)) {
            return currentTenantId;
        }
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<String> roles = userQueryApi.getRolesByUserId(userId);
            if (roles != null && (roles.contains(RoleCode.ADMIN.getCode()) || roles.contains("PLATFORM_ADMIN"))) {
                return requestedTenantId;
            }
        }
        log.warn("🛡️ [防越权] 拦截非超管跨租户访问院系配额: currentTenantId={}, requestedTenantId={}",
                currentTenantId, requestedTenantId);
        return currentTenantId;
    }
}
