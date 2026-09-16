package com.edumind.ai.controller.memory;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.dto.memory.MemoryItemUpdateDTO;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.vo.memory.MemoryDecryptVO;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.ai.vo.memory.MemoryOverviewVO;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
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
    @SaCheckPermission("ai:memory:view")
    public ApiResult<MemoryNamespaceVO> getNamespace(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(agentMemoryService.getNamespace(courseId));
    }

    @PostMapping("/consent")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "更新记忆授权", businessType = BusinessType.GRANT)
    public ApiResult<Void> updateConsent(@Valid @RequestBody MemoryConsentDTO dto) {
        agentMemoryService.updateConsent(dto);
        return ApiResult.success();
    }

    @PostMapping
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "新增记忆条目", businessType = BusinessType.INSERT)
    public ApiResult<Long> createMemory(@Valid @RequestBody MemoryItemCreateDTO dto) {
        return ApiResult.success(agentMemoryService.createMemoryItem(dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "遗忘删除记忆", businessType = BusinessType.DELETE)
    public ApiResult<Void> forgetMemory(@PathVariable("id") Long id) {
        agentMemoryService.forgetMemory(id);
        return ApiResult.success();
    }

    @DeleteMapping("/all")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "清空记忆空间", businessType = BusinessType.CLEAN)
    public ApiResult<Void> forgetAll(@RequestParam(required = false) Long courseId) {
        agentMemoryService.forgetAll(courseId);
        return ApiResult.success();
    }

    @PutMapping("/{id}/feedback")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "记忆有效性反馈", businessType = BusinessType.UPDATE)
    public ApiResult<Void> feedbackMemory(@PathVariable("id") Long id, @Valid @RequestBody MemoryFeedbackDTO dto) {
        agentMemoryService.feedbackMemory(id, dto);
        return ApiResult.success();
    }

    @GetMapping("/overview")
    @SaCheckPermission("ai:memory:view")
    public ApiResult<MemoryOverviewVO> getOverview() {
        return ApiResult.success(agentMemoryService.getMemoryOverview());
    }

    @PostMapping("/seed")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "初始化范本记忆", businessType = BusinessType.INSERT)
    public ApiResult<Integer> seedSampleMemories(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(agentMemoryService.seedSampleMemories(courseId));
    }

    @PostMapping("/extract")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "AI学情智能萃取研判", businessType = BusinessType.OTHER)
    public ApiResult<List<MemoryItemVO>> extractMemories(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(agentMemoryService.extractMemoriesFromActivity(courseId));
    }

    @PostMapping("/batch-confirm")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "确认采纳沉淀记忆", businessType = BusinessType.INSERT)
    public ApiResult<Integer> confirmBatchCandidates(
            @RequestParam(required = false) Long courseId,
            @Valid @RequestBody List<MemoryItemCreateDTO> candidates) {
        return ApiResult.success(agentMemoryService.confirmBatchCandidates(courseId, candidates));
    }

    @GetMapping("/{id}/decrypt")
    @SaCheckPermission("ai:memory:view")
    @OperationLog(module = "Agent长期记忆", title = "国密SM4解密查验", businessType = BusinessType.OTHER)
    public ApiResult<MemoryDecryptVO> decryptMemory(@PathVariable("id") Long id) {
        return ApiResult.success(agentMemoryService.decryptMemory(id));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "更新记忆条目", businessType = BusinessType.UPDATE)
    public ApiResult<Void> updateMemory(@PathVariable("id") Long id, @Valid @RequestBody MemoryItemUpdateDTO dto) {
        agentMemoryService.updateMemoryItem(id, dto);
        return ApiResult.success();
    }

    @GetMapping("/retrieve")
    @SaCheckPermission("ai:memory:view")
    public ApiResult<List<MemoryItemVO>> retrieve(
            @RequestParam(required = false) Long courseId,
            @RequestParam String queryPrompt) {
        return ApiResult.success(agentMemoryService.retrieveMemories(courseId, queryPrompt));
    }

    @PostMapping("/cleanup-duplicates")
    @SaCheckPermission("ai:memory:manage")
    @OperationLog(module = "Agent长期记忆", title = "清理冗余重复记忆", businessType = BusinessType.CLEAN)
    public ApiResult<Integer> cleanupDuplicates(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(agentMemoryService.cleanupDuplicates(courseId));
    }
}
