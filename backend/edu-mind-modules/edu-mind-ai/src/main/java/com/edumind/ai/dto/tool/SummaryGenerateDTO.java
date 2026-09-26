package com.edumind.ai.dto.tool;

import lombok.Data;

/**
 * AI 智能总结生成请求。
 *
 * <p>三种来源任选其一：{@code documentId}（知识库文档正文）或 {@code content}（自由文本）；
 * 二者同时存在时优先使用 {@code content}。</p>
 */
@Data
public class SummaryGenerateDTO {

    /** 关联课程（可选）：用于课程上下文与历史筛选 */
    private Long courseId;

    /** 知识库文档 ID（source_type=DOCUMENT） */
    private Long documentId;

    /** 直接粘贴的文本（source_type=TEXT） */
    private String content;

    /** 总结模式：OVERVIEW / CHAPTER / MISTAKE / REVIEW，缺省 OVERVIEW */
    private String mode;

    /** 自定义标题；为空时后端按「来源名·模式」自动命名 */
    private String title;
}
