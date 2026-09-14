package com.edumind.ai.controller.conversation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.ConversationCreateDTO;
import com.edumind.ai.dto.ConversationRenameDTO;
import com.edumind.ai.service.conversation.ConversationService;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageDeleteResultVO;
import com.edumind.ai.vo.MessageVO;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @SaCheckPermission("ai:chat")
    @GetMapping
    public ApiResult<List<ConversationVO>> listConversations(
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return ApiResult.success(conversationService.listConversations(courseId));
    }

    @SaCheckPermission("ai:chat")
    @PostMapping
    public ApiResult<ConversationVO> createConversation(@Valid @RequestBody ConversationCreateDTO dto) {
        return ApiResult.success(conversationService.createConversation(dto));
    }

    @SaCheckPermission("ai:chat")
    @GetMapping("/{id}/messages")
    public ApiResult<List<MessageVO>> listMessages(@PathVariable("id") String id) {
        return ApiResult.success(conversationService.listMessages(id));
    }

    @SaCheckPermission("ai:chat")
    @PutMapping("/{id}")
    public ApiResult<Void> renameConversation(@PathVariable("id") String id,
                                              @Valid @RequestBody ConversationRenameDTO dto) {
        conversationService.renameConversation(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("ai:chat")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable("id") String id) {
        conversationService.deleteConversation(id);
    }

    @SaCheckPermission("ai:chat")
    @PostMapping("/{id}/generate-title")
    public ApiResult<String> generateTitle(@PathVariable("id") String id) {
        return ApiResult.success(conversationService.generateTitle(id));
    }

    @SaCheckPermission("ai:chat")
    @DeleteMapping("/messages/{messageId}")
    public ApiResult<MessageDeleteResultVO> deleteMessage(@PathVariable("messageId") String messageId) {
        MessageDeleteResultVO result = new MessageDeleteResultVO();
        result.setDeletedIds(conversationService.deleteMessageWithPair(messageId));
        return ApiResult.success(result);
    }
}
