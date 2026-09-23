package com.edumind.ai.controller.chat;

import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.common.api.ApiResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final ChatService chatService;
    private final GatewayManageService gatewayManageService;
    private final com.edumind.ai.service.chat.ChatAttachmentService chatAttachmentService;

    @SaCheckPermission("ai:chat")
    @GetMapping("/models")
    public ApiResult<List<AiModelConfigVO>> listChatModels() {
        return ApiResult.success(gatewayManageService.listEnabledChatModels());
    }

    @SaCheckPermission("ai:chat")
    @PostMapping(value = "/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<com.edumind.ai.vo.chat.ChatAttachmentVO> uploadAttachment(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResult.success(chatAttachmentService.uploadAndParse(file));
    }

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
