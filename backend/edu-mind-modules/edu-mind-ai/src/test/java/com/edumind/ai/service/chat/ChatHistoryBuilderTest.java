package com.edumind.ai.service.chat;

import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.integration.llm.LlmChatMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ChatHistoryBuilderTest {

    private final ChatHistoryBuilder builder = new ChatHistoryBuilder();

    @Test
    void build_shouldKeepChronologicalOrderWithinBudget() {
        List<MessageEntity> messages = new ArrayList<>();
        messages.add(message("user", "第一轮问题"));
        messages.add(message("assistant", "第一轮回答"));
        messages.add(message("user", "第二轮追问"));
        messages.add(message("assistant", "第二轮回答"));

        List<LlmChatMessage> history = builder.build(messages);

        Assertions.assertEquals(4, history.size());
        Assertions.assertEquals("user", history.get(0).getRole());
        Assertions.assertEquals("assistant", history.get(1).getRole());
        Assertions.assertEquals("第二轮追问", history.get(2).getContent());
    }

    @Test
    void withCurrentUserPrompt_shouldReplaceLastUserMessageForRagTurn() {
        List<LlmChatMessage> history = List.of(
                LlmChatMessage.user("原始问题"),
                LlmChatMessage.assistant("上一轮回答"),
                LlmChatMessage.user("原始问题")
        );

        List<LlmChatMessage> merged = builder.withCurrentUserPrompt(history, "【检索上下文】\n原始问题");

        Assertions.assertEquals(3, merged.size());
        Assertions.assertEquals("【检索上下文】\n原始问题", merged.get(2).getContent());
    }

    @Test
    void compactMessageForHistory_shouldPreserveTailMarkers() {
        String longContent = "A".repeat(9000) + "\n### Q1：什么是多态？";
        String compacted = builder.compactMessageForHistory(longContent, 500);

        Assertions.assertTrue(compacted.contains("### Q1"));
        Assertions.assertTrue(compacted.length() <= 560);
    }

    @Test
    void isConversationFollowUp_shouldDetectShortFollowUp() {
        Assertions.assertTrue(builder.isConversationFollowUp("把上面那几道题答案发我"));
        Assertions.assertFalse(builder.isConversationFollowUp("请讲解二叉树的前序遍历"));
    }

    private MessageEntity message(String role, String content) {
        MessageEntity entity = new MessageEntity();
        entity.setRole(role);
        entity.setContent(content);
        return entity;
    }
}
