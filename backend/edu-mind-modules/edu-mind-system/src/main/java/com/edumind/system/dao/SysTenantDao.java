package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysTenantDao {

    private final SysTenantMapper sysTenantMapper;

    public SysTenantEntity findById(Long id) {
        return sysTenantMapper.selectById(id);
    }

    public SysTenantEntity findByCode(String code) {
        if (!StringUtils.hasText(code)) return null;
        return sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getCode, code));
    }

    public List<SysTenantEntity> listAllActive() {
        return sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, 1)
                .orderByDesc(SysTenantEntity::getCreateTime));
    }

    public List<SysTenantEntity> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return sysTenantMapper.selectBatchIds(ids);
    }

    public Page<SysTenantEntity> page(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysTenantEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysTenantEntity::getName, keyword).or().like(SysTenantEntity::getCode, keyword));
        }
        if (status != null) {
            wrapper.eq(SysTenantEntity::getStatus, status);
        }
        wrapper.orderByDesc(SysTenantEntity::getCreateTime);
        return sysTenantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public int insert(SysTenantEntity entity) {
        return sysTenantMapper.insert(entity);
    }

    public int updateById(SysTenantEntity entity) {
        return sysTenantMapper.updateById(entity);
    }

    public long countTotal() {
        return sysTenantMapper.selectCount(null);
    }

    public long countActive() {
        return sysTenantMapper.selectCount(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, 1));
    }

    public int deleteById(Long id) {
        return sysTenantMapper.deleteById(id);
    }
}
