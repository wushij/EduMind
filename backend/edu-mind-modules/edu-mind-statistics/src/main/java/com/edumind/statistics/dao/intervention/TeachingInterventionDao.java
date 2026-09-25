package com.edumind.statistics.dao.intervention;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    /**
     * 状态机条件更新（CAS）：仅当记录当前状态仍等于 expectedStatus 时才流转为 targetStatus。
     *
     * 为什么必须这样做：审批 / 下发属于「先判断状态、再写状态」的典型动作，
     * 用户双击按钮、前端重复提交或网络重放时，两个请求可能同时读到 PENDING，
     * 若各自 updateById 就会重复流转（下发场景还会重复给学生推送通知）。
     * 把状态比较下沉到 SQL 的 WHERE 条件后，由数据库行锁保证只有一个请求能更新成功，
     * 其余请求影响行数为 0，交由上层按幂等语义处理。
     *
     * @param approvedBy   审批人（null 表示不修改该字段）
     * @param proposalJson 新的提案 JSON（null 表示不修改该字段）
     * @return true 表示本次调用真正完成了状态流转
     */
    public boolean updateStatusIfMatch(Long id, String expectedStatus, String targetStatus,
                                       Long approvedBy, String proposalJson) {
        LambdaUpdateWrapper<TeachingInterventionEntity> wrapper = new LambdaUpdateWrapper<TeachingInterventionEntity>()
                .eq(TeachingInterventionEntity::getId, id)
                .eq(TeachingInterventionEntity::getStatus, expectedStatus)
                .set(TeachingInterventionEntity::getStatus, targetStatus);
        if (approvedBy != null) {
            wrapper.set(TeachingInterventionEntity::getApprovedBy, approvedBy);
        }
        if (proposalJson != null) {
            wrapper.set(TeachingInterventionEntity::getProposalJson, proposalJson);
        }
        return teachingInterventionMapper.update(null, wrapper) > 0;
    }

    public int deleteById(Long id) {
        return teachingInterventionMapper.deleteById(id);
    }
}
