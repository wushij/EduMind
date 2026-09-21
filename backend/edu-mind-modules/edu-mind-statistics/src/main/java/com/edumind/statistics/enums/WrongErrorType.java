package com.edumind.statistics.enums;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 错题认知归因类型（存库用 code，展示用 label）。
 */
public enum WrongErrorType {
    CONCEPT("CONCEPT", "概念模糊"),
    LOGIC("LOGIC", "逻辑漏洞"),
    CALC("CALC", "计算失误"),
    READING("READING", "审题不清");

    private final String code;
    private final String label;

    WrongErrorType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static WrongErrorType fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return CONCEPT;
        }
        for (WrongErrorType t : values()) {
            if (t.code.equalsIgnoreCase(code.trim())) {
                return t;
            }
        }
        return CONCEPT;
    }

    /** 指向“学生没作答”的诊断措辞：这类结论无法归因到具体失分类型 */
    private static final List<String> UNANSWERED_HINTS = List.of(
            "未作答", "未填写", "没有作答", "缺答", "作答缺失", "空白作答", "未提交答案", "未提供答案", "答案为空");

    /**
     * 从诊断文本提取失分类型 code。
     * 返回 null 表示「本次不作归因」：诊断为空，或诊断本身指向学生未作答——
     * 否则「作答缺失」会被硬套成审题/计算问题，误导学生的复习方向。
     */
    public static String extractCodeFromDiagnosis(String diagnosis) {
        if (!StringUtils.hasText(diagnosis)) {
            return null;
        }
        for (String hint : UNANSWERED_HINTS) {
            if (diagnosis.contains(hint)) {
                return null;
            }
        }
        String upper = diagnosis.toUpperCase();
        for (WrongErrorType t : values()) {
            if (upper.contains(t.code)) {
                return t.code;
            }
        }
        if (diagnosis.contains("审题") || diagnosis.contains("题意")) {
            return READING.code;
        }
        if (diagnosis.contains("计算") || diagnosis.contains("运算")) {
            return CALC.code;
        }
        if (diagnosis.contains("逻辑") || diagnosis.contains("推理")) {
            return LOGIC.code;
        }
        return CONCEPT.code;
    }

    /**
     * 用于识别「模型输出中的机器标记」的正则集合。
     * 这些标记（如 “类型：CONCEPT”、“（READING）”、“CONCEPT: …”）只是 {@link #extractCodeFromDiagnosis} 的解析依据，
     * 属于内部中间产物，不应出现在面向学生展示的诊断文案里。
     */
    private static final List<Pattern> TYPE_MARKER_PATTERNS = List.of(
            // 开头前缀：CONCEPT: / READING：xxx
            Pattern.compile("^\\s*(CONCEPT|LOGIC|CALC|READING)\\s*[:：]\\s*", Pattern.CASE_INSENSITIVE),
            // 括号包裹：（CONCEPT） / (READING)
            Pattern.compile("[（(]\\s*(CONCEPT|LOGIC|CALC|READING)\\s*[)）]", Pattern.CASE_INSENSITIVE),
            // 显式标注：类型：CONCEPT / 错因类型 CONCEPT
            Pattern.compile("(?:错因)?类型\\s*[:：]?\\s*(CONCEPT|LOGIC|CALC|READING)\\s*[。.；;]?", Pattern.CASE_INSENSITIVE),
            // 归因措辞：属于 CONCEPT / 归为 READING
            Pattern.compile("(?:属于|归为|标记为|判定为|划分为)\\s*(CONCEPT|LOGIC|CALC|READING)\\s*[。.；;]?", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 清洗诊断文案中的英文类型标记，保留纯中文结论。
     * 历史数据里存在 “……。类型：CONCEPT”“……（READING）”“CONCEPT: xxx” 等混杂写法，
     * 写入时与展示时都应统一剥离，避免向学生暴露内部枚举值。
     */
    public static String stripTypeMarker(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String cleaned = text;
        for (Pattern pattern : TYPE_MARKER_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll(" ");
        }
        return cleaned
                // 收敛清洗后残留的重复空格与悬空标点
                .replaceAll("\\s{2,}", " ")
                .replaceAll("\\s+([，,。.；;：:])", "$1")
                .replaceAll("[，,、]\\s*(?=[。.；;]|$)", "")
                // 剥离标记后若只剩悬空的逗号/分号，统一收尾为句号，避免出现 “……” 这类断裂结尾
                .replaceAll("[，,、；;]\\s*$", "。")
                .replaceAll("。{2,}", "。")
                .trim();
    }

    public static List<String> labelsForStoredCodes(String stored) {
        if (!StringUtils.hasText(stored)) {
            return List.of();
        }
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(WrongErrorType::fromCode)
                .map(WrongErrorType::getLabel)
                .distinct()
                .collect(Collectors.toList());
    }
}
