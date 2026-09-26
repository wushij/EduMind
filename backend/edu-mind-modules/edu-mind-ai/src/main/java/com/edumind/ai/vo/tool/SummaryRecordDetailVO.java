package com.edumind.ai.vo.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 智能总结记录详情（在列表字段基础上追加 Markdown 正文）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SummaryRecordDetailVO extends SummaryRecordVO {

    /** 总结正文（Markdown） */
    private String content;
}
