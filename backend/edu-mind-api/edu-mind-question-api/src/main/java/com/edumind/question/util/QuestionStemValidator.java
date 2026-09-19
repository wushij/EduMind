package com.edumind.question.util;

/**
 * 拦截误将 AI 命题提示词、Mock 占位文本写入题干的脏数据。
 */
public final class QuestionStemValidator {

    private QuestionStemValidator() {
    }

    public static boolean isGarbageStem(String stem) {
        if (stem == null || stem.isBlank()) {
            return true;
        }
        String text = stem.trim();
        if (text.startsWith("Mock 题目") || text.contains("Mock 题目")) {
            return true;
        }
        if (text.contains("你是一位专业的教学出题助手")
                && (text.contains("JSON 格式") || text.contains("questions 数组"))) {
            return true;
        }
        if (text.contains("请严格按 JSON 格式输出") && text.contains("questions")) {
            return true;
        }
        if (text.length() > 500
                && text.contains("课程ID=")
                && text.contains("知识点=")
                && text.contains("题型=")) {
            return true;
        }
        return false;
    }
}
