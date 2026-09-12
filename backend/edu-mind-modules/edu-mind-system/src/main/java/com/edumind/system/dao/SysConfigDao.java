package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysConfigEntity;
import com.edumind.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统全局参数配置 DAO 统一访问层
 */
@Repository
@RequiredArgsConstructor
public class SysConfigDao {

    private final SysConfigMapper sysConfigMapper;

    public SysConfigEntity findByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getConfigKey, key.trim()));
    }

    public List<SysConfigEntity> findByGroup(String group) {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getConfigGroup, group));
    }

    public List<SysConfigEntity> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfigEntity>()
                .orderByAsc(SysConfigEntity::getId));
    }

    public int insert(SysConfigEntity entity) {
        return sysConfigMapper.insert(entity);
    }

    public int updateById(SysConfigEntity entity) {
        return sysConfigMapper.updateById(entity);
    }
}
