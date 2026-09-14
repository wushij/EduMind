package com.edumind.ai.service.chat;

import com.edumind.ai.dto.ChatStreamDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    SseEmitter streamChat(ChatStreamDTO dto);

    void cancelStream(String streamId);

    /**
     * 构建包含学生个性化长期记忆的 System Prompt (用于主链路及单测验证)
     */
    String buildSystemPromptWithMemory(String baseSystemPrompt, Long courseId, String queryMessage);
}

