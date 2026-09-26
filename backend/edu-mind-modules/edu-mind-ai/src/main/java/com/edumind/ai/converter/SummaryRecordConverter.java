package com.edumind.ai.converter;

import com.edumind.ai.entity.SummaryRecordEntity;
import com.edumind.ai.service.tool.SummaryMode;
import com.edumind.ai.vo.tool.SummaryRecordDetailVO;
import com.edumind.ai.vo.tool.SummaryRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * AI 智能总结记录 Entity → VO 转换器。
 */
@Component
public class SummaryRecordConverter {

    public SummaryRecordVO toVO(SummaryRecordEntity entity) {
        SummaryRecordVO vo = new SummaryRecordVO();
        copyBase(entity, vo);
        return vo;
    }

    public SummaryRecordDetailVO toDetailVO(SummaryRecordEntity entity) {
        SummaryRecordDetailVO vo = new SummaryRecordDetailVO();
        copyBase(entity, vo);
        vo.setContent(entity.getContent());
        return vo;
    }

    private void copyBase(SummaryRecordEntity entity, SummaryRecordVO vo) {
        BeanUtils.copyProperties(entity, vo);
        // 实体字段 summaryMode 与 VO 字段 mode 命名不同，需显式映射
        vo.setMode(entity.getSummaryMode());
        vo.setModeLabel(SummaryMode.labelOf(entity.getSummaryMode()));
    }
}
