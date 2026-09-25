package com.edumind.statistics.service.teaching.support;

import com.edumind.statistics.enums.WrongErrorType;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 从 AI 错因诊断正文中抽取「可直接执行的教学建议」。
 *
 * <p>教情报告的「针对性教研与干预建议」过去只由错因类型模板拼装，与同一行数据里的
 * AI 诊断正文（{@code wrong_question_record.diagnosis}）毫无关系，于是出现
 * 「诊断说结构变形训练不足、建议却在讲审题」的错位。这里把 AI 结论里的教学补救小节抽出来，
 * 让报告建议与「错题分析」里的诊断同源。</p>
 *
 * <p>抽取规则刻意保守：找不到小节、清洗后过短、或只有诊断没有补救结论时一律返回 {@code null}，
 * 由调用方回落到规则模板，绝不臆造一段建议。</p>
 */
public final class DiagnosisAdviceExtractor {

    /**
     * AI 归因正文中的「干预 / 补救」小节标题。
     *
     * <p>必须与前端结构化渲染（{@code utils/format/structured-text.ts}）使用同一套短语：
     * 模型按诊断 prompt 的要求分点输出（偏差本质 / 教学诊断与补救 …），
     * 前端据此渲染成卡片，报告侧据此抽出可直接执行的教学建议。</p>
     */
    private static final List<String> INTERVENTION_SECTION_TITLES = List.of(
            "教学诊断与补救", "教学干预对策", "补救对策", "教学诊断",
            "靶向微课点拨", "梯度变式训练与提分预期", "梯度变式训练",
            "考点点拨与认知重建", "重点考点点拨", "失分归因与认知障碍诊断");

    /**
     * 诊断正文里可能出现的小节标题全集，用于判定「当前小节到哪里结束」。
     *
     * <p>刻意不按「一、二、」这类编号截断：正文里「增量、分母」这种顿号枚举会被误判成小节边界，
     * 截出半句；只认已知标题，遇到模型自创标题时交给长度收口处理。</p>
     */
    private static final List<String> DIAGNOSIS_SECTION_TITLES = List.of(
            "偏差本质", "失分归因与认知障碍诊断", "认知症结研判",
            "教学诊断与补救", "教学干预对策", "补救对策",
            "靶向微课点拨", "梯度变式训练与提分预期", "梯度变式训练",
            "考点点拨与认知重建", "重点考点点拨");

    /** 建议最大字数：教情报告的建议卡片只展示两三行，过长无收益 */
    private static final int MAX_ADVICE_CHARS = 160;

    /** 清洗后的最小可用字数，低于此值视为抽取失败 */
    private static final int MIN_ADVICE_CHARS = 12;

    private DiagnosisAdviceExtractor() {
    }

    /**
     * 抽取教学干预结论，抽不到返回 {@code null}。
     *
     * @param diagnosis AI 错因诊断正文（可含 {@code [类型: CONCEPT]} 尾标与 LaTeX 公式）
     */
    public static String extract(String diagnosis) {
        if (!StringUtils.hasText(diagnosis)) {
            return null;
        }
        String text = WrongErrorType.stripTypeMarker(diagnosis)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        for (String title : INTERVENTION_SECTION_TITLES) {
            int index = text.indexOf(title);
            if (index < 0) {
                continue;
            }
            String tail = text.substring(index + title.length()).replaceFirst("^[：:\\s]+", "");
            tail = tail.substring(0, nextSectionIndex(tail, title));
            String cleaned = cleanAdviceText(tail);
            if (cleaned.length() >= MIN_ADVICE_CHARS) {
                return truncateAdvice(cleaned);
            }
        }
        return null;
    }

    /** 当前小节正文的结束位置：最近出现的其它已知小节标题 */
    private static int nextSectionIndex(String tail, String currentTitle) {
        int end = tail.length();
        for (String other : DIAGNOSIS_SECTION_TITLES) {
            if (other.equals(currentTitle)) {
                continue;
            }
            int index = tail.indexOf(other);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return end;
    }

    /**
     * 建议正文里不能出现 LaTeX 源码：教情报告的建议卡片是纯文本渲染（不解析公式），
     * 直接透传 {@code \\lim_{x\\to0}\\frac{...}} 会显示成一串符号噪声。
     * 公式被抹掉后留下的孤立空格也要一并收掉，避免出现「将 与 成对配凑」这类断字。
     */
    private static String cleanAdviceText(String raw) {
        return raw
                // 公式定界符 \( \) \[ \] 不保证成对（f(0) 里的括号会打断配对匹配），因此直接整体移除，
                // 不做「配对内容」匹配
                .replaceAll("\\\\[()\\[\\]]", " ")
                .replaceAll("\\$+", " ")
                // \lim \frac \to 之类的命令名与 {} ^ _ 一并抹掉，只留中文与可读的符号
                .replaceAll("\\\\[a-zA-Z]+", " ")
                .replaceAll("[{}^_~]", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("(?<=[\\u4e00-\\u9fa5，。；：、])\\s+(?=[\\u4e00-\\u9fa5，。；：、])", "")
                .replaceAll("\\s+(?=[，。；：、])", "")
                .trim();
    }

    /** 建议按整句收口，避免出现「…训练不足，学生未形」这类半截句 */
    private static String truncateAdvice(String text) {
        if (text.length() <= MAX_ADVICE_CHARS) {
            return text;
        }
        String cut = text.substring(0, MAX_ADVICE_CHARS);
        int stop = Math.max(cut.lastIndexOf('。'), Math.max(cut.lastIndexOf('；'), cut.lastIndexOf('，')));
        return stop >= MAX_ADVICE_CHARS / 2 ? cut.substring(0, stop + 1) : cut + "…";
    }
}
