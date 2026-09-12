package com.edumind.ai.service.chat;

import com.edumind.ai.dto.ChatStreamDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    SseEmitter streamChat(ChatStreamDTO dto);

    void cancelStream(String streamId);
}
