package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.dto.log.SysOperLogPageQueryDTO;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * 操作日志数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class SysOperLogDao {

    private final SysOperLogMapper sysOperLogMapper;

    public int insert(SysOperLogEntity entity) {
        return sysOperLogMapper.insert(entity);
    }

    public SysOperLogEntity selectById(Long id) {
        return sysOperLogMapper.selectById(id);
    }

    public int deleteById(Long id) {
        return sysOperLogMapper.deleteById(id);
    }

    public int deleteBatchIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return sysOperLogMapper.deleteByIds(ids);
    }

    public int cleanByTenant(Long tenantId) {
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        return sysOperLogMapper.delete(wrapper);
    }

    public Page<SysOperLogEntity> pageQuery(Long tenantId, SysOperLogPageQueryDTO query) {
        long pageNo = query.getPageNo() != null && query.getPageNo() > 0 ? query.getPageNo() : 1L;
        long pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
        Page<SysOperLogEntity> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(SysOperLogEntity::getTitle, query.getTitle().trim());
        }
        if (StringUtils.hasText(query.getOperName())) {
            wrapper.like(SysOperLogEntity::getOperName, query.getOperName().trim());
        }
        if (query.getBusinessType() != null) {
            wrapper.eq(SysOperLogEntity::getBusinessType, query.getBusinessType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysOperLogEntity::getStatus, query.getStatus());
        }
        if (query.getOperUserId() != null && query.getOperUserId() > 0) {
            wrapper.eq(SysOperLogEntity::getOperUserId, query.getOperUserId());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            wrapper.ge(SysOperLogEntity::getOperTime, query.getStartTime().trim());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            wrapper.le(SysOperLogEntity::getOperTime, query.getEndTime().trim());
        }

        wrapper.orderByDesc(SysOperLogEntity::getOperTime);
        return sysOperLogMapper.selectPage(page, wrapper);
    }

    public List<SysOperLogEntity> listRecentByUser(Long tenantId, Long userId, int limit) {
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        wrapper.eq(SysOperLogEntity::getOperUserId, userId)
                .orderByDesc(SysOperLogEntity::getOperTime)
                .last("LIMIT " + Math.max(1, limit));
        return sysOperLogMapper.selectList(wrapper);
    }

    public long countTotal(Long tenantId) {
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        return sysOperLogMapper.selectCount(wrapper);
    }

    public long countToday(Long tenantId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        wrapper.ge(SysOperLogEntity::getOperTime, startOfDay)
                .le(SysOperLogEntity::getOperTime, endOfDay);
        return sysOperLogMapper.selectCount(wrapper);
    }

    public long countStatus(Long tenantId, Integer status) {
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        wrapper.eq(SysOperLogEntity::getStatus, status);
        return sysOperLogMapper.selectCount(wrapper);
    }

    public long avgCostTime(Long tenantId) {
        LambdaQueryWrapper<SysOperLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysOperLogEntity::getTenantId, tenantId);
        }
        wrapper.select(SysOperLogEntity::getCostTime)
                .orderByDesc(SysOperLogEntity::getId)
                .last("LIMIT 100");
        List<SysOperLogEntity> list = sysOperLogMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return 0L;
        }
        long sum = list.stream().mapToLong(e -> e.getCostTime() != null ? e.getCostTime() : 0L).sum();
        return sum / list.size();
    }
}
