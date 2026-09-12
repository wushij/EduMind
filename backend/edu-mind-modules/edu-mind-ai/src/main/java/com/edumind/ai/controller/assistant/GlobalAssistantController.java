package com.edumind.ai.controller.assistant;

import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class GlobalAssistantController {

    private final GlobalAssistantService globalAssistantService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody GlobalAssistantRequestDTO dto) {
        return globalAssistantService.streamChat(dto);
    }

    @PostMapping("/ask")
    public ApiResult<Map<String, Object>> ask(@Valid @RequestBody GlobalAssistantRequestDTO dto) {
        return ApiResult.success(globalAssistantService.ask(dto));
    }
}
