package com.edumind.question.dao.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.question.entity.export.ExportTaskEntity;
import com.edumind.question.mapper.export.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ExportTaskDao {

    private final ExportTaskMapper exportTaskMapper;

    public int insert(ExportTaskEntity entity) {
        return exportTaskMapper.insert(entity);
    }

    public ExportTaskEntity findById(Long id) {
        return exportTaskMapper.selectById(id);
    }

    public ExportTaskEntity findByIdAndTenantId(Long id, Long tenantId) {
        return exportTaskMapper.selectOne(new LambdaQueryWrapper<ExportTaskEntity>()
                .eq(ExportTaskEntity::getId, id)
                .eq(ExportTaskEntity::getTenantId, tenantId));
    }

    public List<ExportTaskEntity> listByUserId(Long tenantId, Long userId) {
        return exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTaskEntity>()
                .eq(ExportTaskEntity::getTenantId, tenantId)
                .eq(ExportTaskEntity::getUserId, userId)
                .orderByDesc(ExportTaskEntity::getCreateTime));
    }

    public int updateById(ExportTaskEntity entity) {
        return exportTaskMapper.updateById(entity);
    }
}
