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

        if (lower.contains("卷") || lower.contains("出题") || lower.contains("选题") || lower.contains("习题") || (lower.contains("生成") && lower.contains("题")) || lower.contains("做题")) {
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
        if (lower.contains("教学") || lower.contains("教案") || lower.contains("讲解")) {
            return new IntentResult("agent", "teaching", 0.85, slots);
        }
        return new IntentResult("chat", "general", 0.80, slots);
    }
}
