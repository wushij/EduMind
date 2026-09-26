package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能总结记录。
 *
 * <p>每次生成（无论同步还是流式）都会落一条记录，供历史回看 / 重命名 / 删除 / 导出，
 * 让「一次性 LLM 调用」变成可沉淀的教学资产。</p>
 */
@Data
@TableName("ai_summary_record")
public class SummaryRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    /** DOCUMENT | TEXT */
    private String sourceType;
    private Long documentId;
    private String documentName;
    /** OVERVIEW | CHAPTER | MISTAKE | REVIEW */
    private String summaryMode;
    private String title;
    private String sourceExcerpt;
    /** 总结正文（Markdown） */
    private String content;
    private Integer sourceLength;
    private Integer wordCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
