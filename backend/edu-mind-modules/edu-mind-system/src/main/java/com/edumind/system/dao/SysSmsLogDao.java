package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.entity.SysSmsLogEntity;
import com.edumind.system.mapper.SysSmsLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 短信发送日志统一数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class SysSmsLogDao {

    private final SysSmsLogMapper smsLogMapper;

    public int insert(SysSmsLogEntity entity) {
        return smsLogMapper.insert(entity);
    }

    public List<SysSmsLogEntity> listRecent(int limit) {
        return smsLogMapper.selectList(new LambdaQueryWrapper<SysSmsLogEntity>()
                .orderByDesc(SysSmsLogEntity::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
    }

    public Page<SysSmsLogEntity> page(int pageNo, int pageSize, String phone, Integer status) {
        LambdaQueryWrapper<SysSmsLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.isBlank()) {
            wrapper.like(SysSmsLogEntity::getPhone, phone.trim());
        }
        if (status != null) {
            wrapper.eq(SysSmsLogEntity::getStatus, status);
        }
        wrapper.orderByDesc(SysSmsLogEntity::getCreateTime);
        return smsLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    public SysSmsLogEntity findLatestByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return smsLogMapper.selectOne(new LambdaQueryWrapper<SysSmsLogEntity>()
                .eq(SysSmsLogEntity::getPhone, phone.trim())
                .orderByDesc(SysSmsLogEntity::getCreateTime)
                .last("LIMIT 1"));
    }
}
