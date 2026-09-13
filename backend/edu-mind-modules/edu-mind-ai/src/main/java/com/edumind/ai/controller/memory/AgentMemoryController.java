package com.edumind.ai.controller.memory;

import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/memories")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final AgentMemoryService agentMemoryService;

    @GetMapping
    public ApiResult<MemoryNamespaceVO> getNamespace(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(agentMemoryService.getNamespace(courseId));
    }

    @PostMapping("/consent")
    public ApiResult<Void> updateConsent(@Valid @RequestBody MemoryConsentDTO dto) {
        agentMemoryService.updateConsent(dto);
        return ApiResult.success();
    }

    @PostMapping
    public ApiResult<Long> createMemory(@Valid @RequestBody MemoryItemCreateDTO dto) {
        return ApiResult.success(agentMemoryService.createMemoryItem(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> forgetMemory(@PathVariable("id") Long id) {
        agentMemoryService.forgetMemory(id);
        return ApiResult.success();
    }

    @PutMapping("/{id}/feedback")
    public ApiResult<Void> feedbackMemory(@PathVariable("id") Long id, @Valid @RequestBody MemoryFeedbackDTO dto) {
        agentMemoryService.feedbackMemory(id, dto);
        return ApiResult.success();
    }

    @GetMapping("/retrieve")
    public ApiResult<List<MemoryItemVO>> retrieve(
            @RequestParam(required = false) Long courseId,
            @RequestParam String queryPrompt) {
        return ApiResult.success(agentMemoryService.retrieveMemories(courseId, queryPrompt));
    }
}
