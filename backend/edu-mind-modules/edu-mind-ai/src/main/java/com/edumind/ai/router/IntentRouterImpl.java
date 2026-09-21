package com.edumind.ai.router;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class IntentRouterImpl implements IntentRouter {

    @Override
    public String routeIntent(String input) {
        IntentResult res = route(input, null);
        return res.type();
    }

    @Override
    public IntentResult route(String input, Long courseId) {
        if (input == null || input.isBlank()) {
            return new IntentResult("chat", "general", 1.0, Map.of());
        }
        String lower = input.toLowerCase();
        Map<String, Object> slots = new HashMap<>();
        if (courseId != null) {
            slots.put("courseId", courseId);
        }

        // 1. 题目答疑与解题思路辅导（问怎么解、解题步骤、思路讲解、题目辅导等归为辅导答疑）
        if (lower.contains("思路") || lower.contains("步骤") || lower.contains("讲解") || lower.contains("辅导")
                || lower.contains("解题") || lower.contains("怎么解") || lower.contains("怎么做") || lower.contains("讲讲")
                || lower.contains("为什么选") || lower.contains("题意") || lower.contains("分析选项")) {
            return new IntentResult("chat", "tutor", 0.95, slots);
        }

        // 2. 组卷与出题 Agent（明确要出试卷、组卷、出题）
        if (lower.contains("组卷") || lower.contains("试卷") || lower.contains("出卷") || lower.contains("出一套")
                || lower.contains("出题") || lower.contains("生成试卷") || lower.contains("生成一套")
                || (lower.contains("生成") && lower.contains("题")) || lower.contains("选题")) {
            return new IntentResult("agent", "exam", 0.95, slots);
        }
        if (lower.contains("批改") || lower.contains("评分") || lower.contains("阅卷")) {
            return new IntentResult("agent", "grading", 0.90, slots);
        }
        if (lower.contains("报表") || (lower.contains("看") && lower.contains("分析")) || lower.contains("前往") || lower.contains("导航")) {
            return new IntentResult("navigate", "/analytics/overview", 0.90, slots);
        }
        if (lower.contains("学情") || lower.contains("薄弱") || lower.contains("错题") || lower.contains("掌握度")) {
            return new IntentResult("agent", "learning", 0.91, slots);
        }
        if (lower.contains("检索") || lower.contains("资料") || lower.contains("知识库") || lower.contains("查找") || lower.contains("切片")) {
            return new IntentResult("rag", "kb_retrieval", 0.88, slots);
        }
        if (lower.contains("教学") || lower.contains("教案")) {
            return new IntentResult("agent", "teaching", 0.85, slots);
        }
        return new IntentResult("chat", "general", 0.80, slots);
    }
}
