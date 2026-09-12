package com.edumind.ai.controller.chat;

import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.service.chat.ChatService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final ChatService chatService;

    @SaCheckPermission("ai:chat")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatStreamDTO dto) {
        return chatService.streamChat(dto);
    }

    @SaCheckPermission("ai:chat")
    @DeleteMapping("/stream/{streamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStream(@PathVariable("streamId") String streamId) {
        chatService.cancelStream(streamId);
    }
}
