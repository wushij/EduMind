package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.entity.SysEmailLogEntity;
import com.edumind.system.mapper.SysEmailLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邮件发送日志统一数据访问层 DAO
 */
@Repository
@RequiredArgsConstructor
public class SysEmailLogDao {

    private final SysEmailLogMapper emailLogMapper;

    public int insert(SysEmailLogEntity entity) {
        return emailLogMapper.insert(entity);
    }

    public List<SysEmailLogEntity> listRecent(int limit) {
        return emailLogMapper.selectList(new LambdaQueryWrapper<SysEmailLogEntity>()
                .orderByDesc(SysEmailLogEntity::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
    }

    public Page<SysEmailLogEntity> page(int pageNo, int pageSize, String email, Integer status) {
        LambdaQueryWrapper<SysEmailLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (email != null && !email.isBlank()) {
            wrapper.like(SysEmailLogEntity::getEmail, email.trim());
        }
        if (status != null) {
            wrapper.eq(SysEmailLogEntity::getStatus, status);
        }
        wrapper.orderByDesc(SysEmailLogEntity::getCreateTime);
        return emailLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }
}
