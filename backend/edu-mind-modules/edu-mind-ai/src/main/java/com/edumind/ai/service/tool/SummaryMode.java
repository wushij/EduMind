package com.edumind.ai.service.tool;

/**
 * AI 总结模式：前端展示标签 + 该模式的<b>专属提示词模板编码</b>。
 *
 * <p>每种模式对应一个独立的 Prompt 模板（可在「AI 运维 → Prompt 模板库」管理与版本化），
 * 而不是把四种模式的差异塞进同一段提示词里拼接——后者会让模型输出退回通用摘要、
 * 四种模式正文千篇一律。模板缺失时由 Service 兜底。</p>
 */
public enum SummaryMode {

    OVERVIEW("全文速览", "SUMMARY_OVERVIEW"),
    CHAPTER("章节要点", "SUMMARY_CHAPTER"),
    MISTAKE("易错清单", "SUMMARY_MISTAKE"),
    REVIEW("复习精要", "SUMMARY_REVIEW");

    private final String label;
    /** 专属提示词模板编码：对应 prompt_template.code，同时兜底 classpath:prompt/&lt;code&gt;.st */
    private final String templateCode;

    SummaryMode(String label, String templateCode) {
        this.label = label;
        this.templateCode = templateCode;
    }

    public String getLabel() {
        return label;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    /** 容错解析：未知/空值统一回落到「全文速览」，保证不会因前端传参异常而中断生成。 */
    public static SummaryMode from(String code) {
        if (code == null || code.isBlank()) {
            return OVERVIEW;
        }
        String normalized = code.trim().toUpperCase();
        for (SummaryMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return OVERVIEW;
    }

    public static String labelOf(String code) {
        return from(code).getLabel();
    }
}
