package com.edumind.system.service.tenant.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.converter.TenantConverter;
import com.edumind.system.dao.SysCampusDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dto.tenant.CampusCreateDTO;
import com.edumind.system.dto.tenant.CampusUpdateDTO;
import com.edumind.system.entity.SysCampusEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.service.tenant.TenantCampusService;
import com.edumind.system.vo.tenant.CampusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantCampusServiceImpl implements TenantCampusService {

    private final SysCampusDao sysCampusDao;
    private final SysOrganizationDao sysOrganizationDao;
    private final TenantConverter tenantConverter;

    @Override
    public List<CampusVO> listCampuses(Long tenantId) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Long tid = tenantId != null ? tenantId : TenantContext.requireTenantId();
            List<SysCampusEntity> campuses = sysCampusDao.listAllByTenantId(tid);
            return campuses.stream().map(tenantConverter::toCampusVO).collect(Collectors.toList());
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCampus(Long tenantId, CampusCreateDTO dto) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Long tid = tenantId != null ? tenantId : TenantContext.requireTenantId();
            SysCampusEntity exist = sysCampusDao.findByCode(tid, dto.getCode());
            if (exist != null) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "该校区编码在当前租户下已存在: " + dto.getCode());
            }

            SysCampusEntity campus = new SysCampusEntity();
            campus.setTenantId(tid);
            campus.setCode(dto.getCode().trim().toUpperCase());
            campus.setName(dto.getName().trim());
            campus.setAddress(dto.getAddress());
            campus.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            sysCampusDao.insert(campus);

            SysOrganizationEntity org = new SysOrganizationEntity();
            org.setTenantId(tid);
            org.setParentId(0L);
            org.setOrgType("CAMPUS");
            org.setName(dto.getName().trim());
            org.setSortOrder(1);
            org.setOrgPath("1");
            sysOrganizationDao.insert(org);
            org.setOrgPath(String.valueOf(org.getId()));
            sysOrganizationDao.updateById(org);

            return campus.getId();
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCampus(Long tenantId, Long campusId, CampusUpdateDTO dto) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Long tid = tenantId != null ? tenantId : TenantContext.requireTenantId();
            SysCampusEntity campus = sysCampusDao.findById(campusId);
            if (campus == null || !tid.equals(campus.getTenantId())) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "校区不存在");
            }
            campus.setName(dto.getName().trim());
            campus.setAddress(dto.getAddress());
            if (dto.getStatus() != null) {
                campus.setStatus(dto.getStatus());
            }
            sysCampusDao.updateById(campus);
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    public void updateCampusStatus(Long tenantId, Long campusId, Integer status) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Long tid = tenantId != null ? tenantId : TenantContext.requireTenantId();
            SysCampusEntity campus = sysCampusDao.findById(campusId);
            if (campus == null || !tid.equals(campus.getTenantId())) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "校区不存在");
            }
            campus.setStatus(status != null && status == 1 ? 1 : 0);
            sysCampusDao.updateById(campus);
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampus(Long tenantId, Long campusId) {
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Long tid = tenantId != null ? tenantId : TenantContext.requireTenantId();
            SysCampusEntity campus = sysCampusDao.findById(campusId);
            if (campus == null || !tid.equals(campus.getTenantId())) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "校区不存在");
            }
            long count = sysCampusDao.countByTenantId(tid);
            if (count <= 1) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "至少保留一个校区，不可删除唯一校区");
            }
            sysCampusDao.deleteById(campusId);
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }
}
