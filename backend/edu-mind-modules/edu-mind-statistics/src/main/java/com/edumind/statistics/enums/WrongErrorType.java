package com.edumind.statistics.enums;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
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

    public static String extractCodeFromDiagnosis(String diagnosis) {
        if (!StringUtils.hasText(diagnosis)) {
            return CONCEPT.code;
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
