package com.edumind.statistics.dao.intervention;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.statistics.entity.intervention.TeachingInterventionEntity;
import com.edumind.statistics.mapper.intervention.TeachingInterventionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TeachingInterventionDao {

    private final TeachingInterventionMapper teachingInterventionMapper;

    public List<TeachingInterventionEntity> listByTenantAndCourse(Long tenantId, Long courseId) {
        LambdaQueryWrapper<TeachingInterventionEntity> wrapper = new LambdaQueryWrapper<TeachingInterventionEntity>()
                .eq(TeachingInterventionEntity::getTenantId, tenantId);
        if (courseId != null) {
            wrapper.eq(TeachingInterventionEntity::getCourseId, courseId);
        }
        wrapper.orderByDesc(TeachingInterventionEntity::getCreateTime);
        return teachingInterventionMapper.selectList(wrapper);
    }

    public TeachingInterventionEntity findByIdAndTenantId(Long id, Long tenantId) {
        return teachingInterventionMapper.selectOne(new LambdaQueryWrapper<TeachingInterventionEntity>()
                .eq(TeachingInterventionEntity::getId, id)
                .eq(TeachingInterventionEntity::getTenantId, tenantId));
    }

    public TeachingInterventionEntity findByIdIgnoreTenant(Long id) {
        final TeachingInterventionEntity[] holder = new TeachingInterventionEntity[1];
        com.edumind.common.context.TenantContext.runWithoutTenant(() -> holder[0] = teachingInterventionMapper.selectById(id));
        return holder[0];
    }

    public int insert(TeachingInterventionEntity entity) {
        return teachingInterventionMapper.insert(entity);
    }

    public int updateById(TeachingInterventionEntity entity) {
        return teachingInterventionMapper.updateById(entity);
    }
}
