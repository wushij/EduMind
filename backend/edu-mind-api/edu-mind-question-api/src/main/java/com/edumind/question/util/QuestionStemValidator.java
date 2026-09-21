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

    /**
     * 题干是否明显被截断 / 未闭合（大模型输出被 max_tokens 截断时的典型表现）。
     *
     * <p>只做保守判定，避免误伤正常题干：</p>
     * <ol>
     *   <li>以「逗号/顿号/冒号/分号/左括号」结尾 —— 句子没说完，例如
     *       {@code 现有类Animal及其子类Dog，Animal中定义了方法void sound()，}；</li>
     *   <li>中英文括号或中文引号不配对 —— 括号没闭合。</li>
     * </ol>
     */
    public static boolean isTruncatedStem(String stem) {
        if (stem == null || stem.isBlank()) {
            return true;
        }
        String text = stem.trim();
        char last = text.charAt(text.length() - 1);
        if ("，,、：:；;（(【[《〔<".indexOf(last) >= 0) {
            return true;
        }
        return countOf(text, '（') != countOf(text, '）')
                || countOf(text, '(') != countOf(text, ')')
                || countOf(text, '“') != countOf(text, '”')
                || countOf(text, '《') != countOf(text, '》');
    }

    private static int countOf(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
