package com.edumind.ai.service.chat;

import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.prompt.AiPromptConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多轮对话历史压缩与滑动窗口（对齐 Code Compass copilot_history.go）。
 */
@Component
public class ChatHistoryBuilder {

    public static final int MAX_HISTORY_MESSAGES = 12;
    public static final int MAX_HISTORY_TOTAL_CHARS = 16000;
    public static final int MAX_HISTORY_PER_MSG_CHARS = 8000;

    private static final String[] HISTORY_TAIL_MARKERS = {
            "### Q1", "### Q1：", "Q1：",
            "## 追问", "连环追问", "变式训练"
    };

    private static final String[] FOLLOW_UP_KEYWORDS = {
            "上面", "上一轮", "上轮", "刚才", "前面", "这几道", "这三道", "那三道", "上述",
            "发我答案", "给我答案", "答案发我", "逐题回答", "刚才那", "你写的", "你列的",
            "刚才说的", "上一个", "继续讲", "接着"
    };

    /**
     * 将数据库消息转为 LLM 多轮上下文（仅 content，不含 reasoning）。
     */
    public List<LlmChatMessage> build(List<MessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<CompactedMessage> compacted = new ArrayList<>();
        for (MessageEntity message : messages) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "assistant" : "user";
            String content = compactMessageForHistory(message.getContent(), MAX_HISTORY_PER_MSG_CHARS);
            compacted.add(new CompactedMessage(role, content, message.getContent()));
        }
        if (compacted.isEmpty()) {
            return new ArrayList<>();
        }

        List<LlmChatMessage> result = new ArrayList<>();
        int totalChars = 0;
        for (int i = compacted.size() - 1; i >= 0; i--) {
            CompactedMessage item = compacted.get(i);
            int charLen = item.content.length();

            if (totalChars + charLen > MAX_HISTORY_TOTAL_CHARS) {
                if (result.isEmpty()) {
                    int budget = Math.max(2000, MAX_HISTORY_TOTAL_CHARS - 200);
                    String forced = item.original.length() > budget
                            ? compactMessageForHistory(item.original, budget)
                            : item.content;
                    result.add(0, new LlmChatMessage(item.role, forced));
                }
                break;
            }

            totalChars += charLen;
            result.add(0, new LlmChatMessage(item.role, item.content));
        }
        return result;
    }

    /**
     * 供 Query Rewrite 使用的轻量历史文本（不含当前轮 userPrompt）。
     */
    public String formatConversationHistory(List<MessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MessageEntity message : messages) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "Assistant" : "User";
            String content = compactMessageForHistory(message.getContent(), 500);
            builder.append('[').append(role).append("]: ").append(content).append('\n');
        }
        return builder.toString().trim();
    }

    public List<LlmChatMessage> withCurrentUserPrompt(List<LlmChatMessage> history, String userPrompt) {
        if (!StringUtils.hasText(userPrompt)) {
            return history != null ? history : Collections.emptyList();
        }
        List<LlmChatMessage> result = history != null ? new ArrayList<>(history) : new ArrayList<>();
        if (!result.isEmpty() && "user".equals(result.get(result.size() - 1).getRole())) {
            result.set(result.size() - 1, LlmChatMessage.user(userPrompt));
        } else {
            result.add(LlmChatMessage.user(userPrompt));
        }
        return result;
    }

    /**
     * 追问上一轮时追加纪律提示，避免模型重复整段 RAG 导读。
     */
    public String appendFollowUpDiscipline(String systemPrompt, String userMessage) {
        if (!StringUtils.hasText(systemPrompt) || !isConversationFollowUp(userMessage)) {
            return systemPrompt;
        }
        return systemPrompt + AiPromptConstants.CHAT_FOLLOW_UP_DISCIPLINE;
    }

    public boolean isConversationFollowUp(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String trimmed = message.trim();
        for (String keyword : FOLLOW_UP_KEYWORDS) {
            if (trimmed.contains(keyword)) {
                return true;
            }
        }
        return trimmed.length() <= 48 && (trimmed.contains("答案") || trimmed.contains("追问"));
    }

    String compactMessageForHistory(String content, int maxChars) {
        String trimmed = content.trim();
        if (maxChars <= 0 || trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }

        for (String marker : HISTORY_TAIL_MARKERS) {
            int idx = trimmed.indexOf(marker);
            if (idx < 0) {
                continue;
            }
            String tail = trimmed.substring(idx).trim();
            int remain = maxChars - 40;
            if (tail.length() <= remain) {
                int headBudget = remain - tail.length();
                if (headBudget > 400) {
                    return trimmed.substring(0, headBudget) + "\n\n…（前文已省略）…\n\n" + tail;
                }
                return tail;
            }
            return tail.substring(0, remain) + "…（追问部分已截断）";
        }

        int headLen = maxChars / 3;
        int tailLen = maxChars - headLen - 24;
        if (tailLen < 0) {
            tailLen = 0;
        }
        if (headLen + tailLen > trimmed.length()) {
            return trimmed.substring(0, maxChars) + "…";
        }
        return trimmed.substring(0, headLen) + "\n\n…（中间已省略）…\n\n" + trimmed.substring(trimmed.length() - tailLen);
    }

    private record CompactedMessage(String role, String content, String original) {
    }
}
