package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class WrongBookOverviewVO {
    /** 待攻坚错题数 */
    private long pendingCount;
    /** 薄弱知识考点数（掌握度低于 70% 的去重考点） */
    private long weakKnowledgePointCount;
    /** 已攻克题量 */
    private long masteredCount;
    /**
     * 变式攻克率（0~100）：已有变式题的记录中，已攻克或对应考点掌握度≥70% 的占比
     */
    private int variantConquerRatePercent;
}
