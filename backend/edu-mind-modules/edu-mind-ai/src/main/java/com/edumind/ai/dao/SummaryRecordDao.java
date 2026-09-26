package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.SummaryRecordEntity;
import com.edumind.ai.mapper.SummaryRecordMapper;
import com.edumind.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 智能总结记录数据访问入口。
 *
 * <p>所有查询强制以 user_id 为第一过滤条件：总结记录属于用户私有资产，
 * 任何跨用户读取都应被视为越权（Service 层也会做归属校验二次兜底）。</p>
 */
@Repository
@RequiredArgsConstructor
public class SummaryRecordDao {

    private final SummaryRecordMapper summaryRecordMapper;

    public SummaryRecordEntity findById(Long id) {
        return id == null ? null : summaryRecordMapper.selectById(id);
    }

    public List<SummaryRecordEntity> listByUser(Long userId, Long courseId, String keyword) {
        LambdaQueryWrapper<SummaryRecordEntity> wrapper = new LambdaQueryWrapper<SummaryRecordEntity>()
                .eq(SummaryRecordEntity::getUserId, userId)
                .eq(courseId != null, SummaryRecordEntity::getCourseId, courseId)
                .orderByDesc(SummaryRecordEntity::getCreateTime)
                .orderByDesc(SummaryRecordEntity::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SummaryRecordEntity::getTitle, kw)
                    .or().like(SummaryRecordEntity::getDocumentName, kw));
        }
        return summaryRecordMapper.selectList(wrapper);
    }

    public int insert(SummaryRecordEntity entity) {
        if (entity.getTenantId() == null
                && TenantContext.getTenantId() != null
                && TenantContext.getTenantId() > 0) {
            entity.setTenantId(TenantContext.getTenantId());
        }
        return summaryRecordMapper.insert(entity);
    }

    public int updateById(SummaryRecordEntity entity) {
        return summaryRecordMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return summaryRecordMapper.deleteById(id);
    }

    public long countByUser(Long userId) {
        return summaryRecordMapper.selectCount(new LambdaQueryWrapper<SummaryRecordEntity>()
                .eq(SummaryRecordEntity::getUserId, userId));
    }
}
