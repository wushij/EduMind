package com.edumind.system.service.log.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.context.TenantContext;
import com.edumind.system.converter.SysOperLogConverter;
import com.edumind.system.dao.SysOperLogDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dto.log.SysOperLogPageQueryDTO;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.service.log.SysOperLogService;
import com.edumind.system.vo.log.SysOperLogStatsVO;
import com.edumind.system.vo.log.SysOperLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl implements SysOperLogService {

    private final SysOperLogDao sysOperLogDao;
    private final SysOperLogConverter sysOperLogConverter;
    private final UserDao userDao;

    @Override
    public Page<SysOperLogVO> page(SysOperLogPageQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        Page<SysOperLogEntity> entityPage = sysOperLogDao.pageQuery(tenantId, query);

        List<SysOperLogVO> voList = sysOperLogConverter.toVOList(entityPage.getRecords());
        enrichOperatorInfo(voList);

        Page<SysOperLogVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public SysOperLogVO getById(Long id) {
        SysOperLogEntity entity = sysOperLogDao.selectById(id);
        if (entity == null) {
            return null;
        }
        SysOperLogVO vo = sysOperLogConverter.toVO(entity);
        enrichOperatorInfo(Collections.singletonList(vo));
        return vo;
    }

    @Override
    public void recordLog(SysOperLogEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getTenantId() == null || entity.getTenantId() <= 0) {
            entity.setTenantId(1L);
        }
        try {
            sysOperLogDao.insert(entity);
        } catch (Exception e) {
            log.error("保存操作日志异常", e);
        }
    }

    @Override
    public void delete(Long id) {
        sysOperLogDao.deleteById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            sysOperLogDao.deleteBatchIds(ids);
        }
    }

    @Override
    public void clean() {
        Long tenantId = TenantContext.getTenantId();
        sysOperLogDao.cleanByTenant(tenantId);
    }

    @Override
    public List<SysOperLogVO> listRecentByUser(Long userId, int limit) {
        Long tenantId = TenantContext.getTenantId();
        List<SysOperLogEntity> entities = sysOperLogDao.listRecentByUser(tenantId, userId, limit);
        List<SysOperLogVO> voList = sysOperLogConverter.toVOList(entities);
        enrichOperatorInfo(voList);
        return voList;
    }

    @Override
    public SysOperLogStatsVO getStats() {
        Long tenantId = TenantContext.getTenantId();
        long total = sysOperLogDao.countTotal(tenantId);
        long today = sysOperLogDao.countToday(tenantId);
        long error = sysOperLogDao.countStatus(tenantId, 1);
        double successRate = total > 0 ? Math.round(((double) (total - error) / total) * 1000.0) / 10.0 : 100.0;
        long avgCost = sysOperLogDao.avgCostTime(tenantId);

        return SysOperLogStatsVO.builder()
                .totalCount(total)
                .todayCount(today)
                .successRate(successRate)
                .errorCount(error)
                .avgCostTime(avgCost)
                .build();
    }

    /**
     * 关联用户表，富化操作人员的真实姓名与院系信息（对标并超越 E:\wu-admin）
     */
    private void enrichOperatorInfo(List<SysOperLogVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> userIds = list.stream()
                .map(SysOperLogVO::getOperUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, UserEntity> userMap = new HashMap<>();
        for (Long uid : userIds) {
            UserEntity u = userDao.findById(uid);
            if (u != null) {
                userMap.put(uid, u);
            }
        }

        for (SysOperLogVO vo : list) {
            if (vo.getOperUserId() != null && userMap.containsKey(vo.getOperUserId())) {
                UserEntity u = userMap.get(vo.getOperUserId());
                String realName = StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
                vo.setOperName(realName);
            }
        }
    }
}
