package com.edumind.question.dao.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.context.TenantContext;
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

    public ExportTaskEntity findByIdIgnoreTenant(Long id) {
        final ExportTaskEntity[] holder = new ExportTaskEntity[1];
        TenantContext.runWithoutTenant(() -> holder[0] = exportTaskMapper.selectById(id));
        return holder[0];
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

    public Page<ExportTaskEntity> pageByUserId(Long tenantId, Long userId, long pageNum, long pageSize) {
        Page<ExportTaskEntity> page = new Page<>(pageNum, pageSize);
        return exportTaskMapper.selectPage(page, new LambdaQueryWrapper<ExportTaskEntity>()
                .eq(ExportTaskEntity::getTenantId, tenantId)
                .eq(ExportTaskEntity::getUserId, userId)
                .orderByDesc(ExportTaskEntity::getCreateTime));
    }

    public int updateById(ExportTaskEntity entity) {
        return exportTaskMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return exportTaskMapper.deleteById(id);
    }
}
