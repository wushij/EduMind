package com.edumind.ai.controller.assistant;

import com.edumind.ai.dto.assistant.FollowUpSuggestDTO;
import com.edumind.ai.dto.assistant.GlobalAssistantRequestDTO;
import com.edumind.ai.service.assistant.FollowUpSuggestService;
import com.edumind.ai.service.assistant.GlobalAssistantService;
import com.edumind.ai.vo.assistant.FollowUpSuggestVO;
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
    private final FollowUpSuggestService followUpSuggestService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody GlobalAssistantRequestDTO dto) {
        return globalAssistantService.streamChat(dto);
    }

    @PostMapping("/ask")
    public ApiResult<Map<String, Object>> ask(@Valid @RequestBody GlobalAssistantRequestDTO dto) {
        return ApiResult.success(globalAssistantService.ask(dto));
    }

    /**
     * 生成「下一步追问」：入参为本轮用户提问与助手回答，由模型依据真实内容现生成。
     *
     * <p>聊天主流程与追问生成解耦：回答先落地展示，追问随后异步补齐；模型不可用时返回
     * {@code aiGenerated=false} 的空列表，前端自动回落到本地规则，不影响会话可用性。</p>
     */
    @PostMapping("/follow-ups")
    public ApiResult<FollowUpSuggestVO> suggestFollowUps(@Valid @RequestBody FollowUpSuggestDTO dto) {
        return ApiResult.success(followUpSuggestService.suggest(dto));
    }
}
