package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysMenuEntity;
import com.edumind.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysMenuDao {

    private final SysMenuMapper sysMenuMapper;

    public SysMenuEntity findById(Long id) {
        return sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getId, id)
                .eq(SysMenuEntity::getDeleted, 0));
    }

    public List<SysMenuEntity> listActive(String keyword) {
        LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getDeleted, 0)
                .orderByAsc(SysMenuEntity::getSort)
                .orderByAsc(SysMenuEntity::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysMenuEntity::getName, keyword.trim());
        }
        return sysMenuMapper.selectList(wrapper);
    }

    public long countActiveChildren(Long parentId) {
        return sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getParentId, parentId)
                .eq(SysMenuEntity::getDeleted, 0));
    }

    public int insert(SysMenuEntity entity) {
        return sysMenuMapper.insert(entity);
    }

    public int updateById(SysMenuEntity entity) {
        return sysMenuMapper.updateById(entity);
    }

    public int softDeleteById(Long id) {
        SysMenuEntity patch = new SysMenuEntity();
        patch.setId(id);
        patch.setDeleted(1);
        patch.setStatus(0);
        return sysMenuMapper.updateById(patch);
    }

    public int deleteAllForTest() {
        return sysMenuMapper.delete(new LambdaQueryWrapper<>());
    }
}
