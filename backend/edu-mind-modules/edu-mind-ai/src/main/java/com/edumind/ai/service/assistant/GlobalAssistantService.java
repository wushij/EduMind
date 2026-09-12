package com.edumind.ai.service.assistant;

import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface GlobalAssistantService {

    SseEmitter streamChat(GlobalAssistantRequestDTO dto);

    Map<String, Object> ask(GlobalAssistantRequestDTO dto);
}
