package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.SysAiQuotaEntity;
import com.edumind.ai.mapper.SysAiQuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysAiQuotaDao {

    private final SysAiQuotaMapper sysAiQuotaMapper;

    public List<SysAiQuotaEntity> listAll() {
        return sysAiQuotaMapper.selectList(new LambdaQueryWrapper<>());
    }

    public SysAiQuotaEntity findByUserId(Long userId) {
        return sysAiQuotaMapper.selectOne(
                new LambdaQueryWrapper<SysAiQuotaEntity>().eq(SysAiQuotaEntity::getUserId, userId)
        );
    }

    public int insert(SysAiQuotaEntity entity) {
        return sysAiQuotaMapper.insert(entity);
    }

    public int updateById(SysAiQuotaEntity entity) {
        return sysAiQuotaMapper.updateById(entity);
    }
}
