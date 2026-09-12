package com.edumind.ai.router;

import org.springframework.stereotype.Service;

@Service
public class IntentRouterImpl implements IntentRouter {

    @Override
    public String routeIntent(String input) {
        if (input == null) {
            return "chat";
        }
        String lower = input.toLowerCase();
        if (lower.contains("出题") || lower.contains("生成") && lower.contains("题")) {
            return "agent";
        }
        if (lower.contains("批改") || lower.contains("评分")) {
            return "agent";
        }
        if (lower.contains("学习") || lower.contains("薄弱")) {
            return "agent";
        }
        if (lower.contains("检索") || lower.contains("资料")) {
            return "rag";
        }
        return "chat";
    }
}
