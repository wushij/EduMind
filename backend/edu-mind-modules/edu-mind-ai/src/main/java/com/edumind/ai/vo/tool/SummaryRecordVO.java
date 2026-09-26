package com.edumind.ai.vo.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能总结记录（列表视图，<b>不含正文</b>，避免历史列表一次性拉回大段 Markdown）。
 */
@Data
public class SummaryRecordVO {

    private Long id;
    private Long courseId;
    private String courseName;
    /** DOCUMENT | TEXT */
    private String sourceType;
    private Long documentId;
    private String documentName;
    /** OVERVIEW | CHAPTER | MISTAKE | REVIEW */
    private String mode;
    private String modeLabel;
    private String title;
    /** 原始资料摘要片段（前 500 字），用于列表/详情预览 */
    private String sourceExcerpt;
    private Integer sourceLength;
    private Integer wordCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
