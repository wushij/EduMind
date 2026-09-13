package com.edumind.ai.util;

import org.springframework.util.StringUtils;

public final class ReasoningEffortNormalizer {

    private ReasoningEffortNormalizer() {
    }

    public static String normalize(String effort) {
        if (!StringUtils.hasText(effort)) {
            return "low";
        }
        return switch (effort.trim().toLowerCase()) {
            case "medium", "high", "max" -> effort.trim().toLowerCase();
            default -> "low";
        };
    }
}
