package com.edumind.ai.service.routing;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 对齐写作伴侣：寒暄/过短闲聊不触发知识库检索，避免无关引用。
 */
public final class CopilotRagPolicy {

    private static final Set<String> GREETINGS = Set.of(
            "你好", "您好", "嗨", "哈喽", "hello", "hi", "hey",
            "在吗", "在不在", "早上好", "下午好", "晚上好", "早安", "晚安",
            "谢谢", "感谢", "多谢", "thanks", "thank you",
            "好的", "好", "嗯", "哦", "ok", "okay", "行", "收到",
            "你是谁", "介绍自己"
    );

    private CopilotRagPolicy() {
    }

    public static boolean shouldSkipRag(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return true;
        }
        String q = rawMessage.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        for (String g : GREETINGS) {
            if (lower.equals(g)) {
                return true;
            }
            if (lower.startsWith(g) && q.length() <= g.length() + 2) {
                return true;
            }
        }
        if (q.length() <= 8 && !hasTechnicalSignal(q)) {
            return true;
        }
        return false;
    }

    private static boolean hasTechnicalSignal(String s) {
        if (s.indexOf('`') >= 0 || s.indexOf('<') >= 0 || s.indexOf('{') >= 0) {
            return true;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.contains("api") || lower.contains("sql") || lower.contains("java")
                || lower.contains("python") || lower.contains("vue") || lower.contains("错误")
                || lower.contains("代码") || lower.contains("原理") || lower.contains("为什么");
    }
}
