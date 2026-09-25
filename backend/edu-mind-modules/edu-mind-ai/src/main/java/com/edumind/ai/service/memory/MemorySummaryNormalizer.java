package com.edumind.ai.service.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 长期记忆摘要的文本规范化：入库前统一做长度收口。
 *
 * <p>为什么必须有这一层：{@code ai_memory_item.summary} 有三条写入来源，其中两条长度不可控——
 * 大模型生成的候选摘要（{@link #normalize} 的调用方批量确认落库）、
 * 以及用户纠错反馈里提交的修正内容（会直接覆盖 summary）。
 * 历史上该列曾是 VARCHAR(512)，超长时 MySQL 严格模式直接拒绝写入，
 * 被全局异常处理器转成 400「数据操作冲突」，调用方完全看不出是"内容太长"。
 *
 * <p>截断策略：优先在句末「。」或换行处收口，避免把半句话（或 Markdown/公式片段）截断后
 * 展示成残缺摘要；确实找不到边界时才退化为按字符硬截断。
 */
@Slf4j
@Component
public class MemorySummaryNormalizer {

    /** 摘要入库长度上限（字符）。列已是 TEXT，此处是应用层纪律，避免无限膨胀的摘要污染展示与检索 */
    public static final int MAX_SUMMARY_CHARS = 1000;

    /**
     * 生成可安全入库的摘要文本：去首尾空白 + 超长按句边界截断。
     * 入参为 null 时原样返回 null，避免把"未传摘要"误写成空串。
     */
    public String toStorageText(String summary) {
        if (summary == null) {
            return null;
        }
        String trimmed = summary.trim();
        if (trimmed.length() <= MAX_SUMMARY_CHARS) {
            return trimmed;
        }
        int cut = trimmed.lastIndexOf('。', MAX_SUMMARY_CHARS);
        if (cut < MAX_SUMMARY_CHARS / 2) {
            int lineCut = trimmed.lastIndexOf('\n', MAX_SUMMARY_CHARS);
            if (lineCut > cut) {
                cut = lineCut;
            }
        }
        if (cut <= 0) {
            cut = MAX_SUMMARY_CHARS;
        }
        log.warn("[长期记忆] 摘要 {} 字，超过入库上限 {} 字，已按句边界截断保存",
                trimmed.length(), MAX_SUMMARY_CHARS);
        return trimmed.substring(0, cut) + "……";
    }
}
