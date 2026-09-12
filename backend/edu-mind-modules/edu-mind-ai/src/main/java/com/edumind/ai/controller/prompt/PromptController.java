package com.edumind.ai.controller.prompt;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.prompt.PromptTemplateDTO;
import com.edumind.ai.dto.prompt.PromptTestDTO;
import com.edumind.ai.service.prompt.PromptManageService;
import com.edumind.ai.vo.prompt.PromptTemplateVersionVO;
import com.edumind.ai.vo.prompt.PromptTemplateVO;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptManageService promptManageService;

    @SaCheckPermission("system:prompt:view")
    @GetMapping
    public ApiResult<List<PromptTemplateVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.success(promptManageService.list(category, status, keyword));
    }

    @SaCheckPermission("system:prompt:view")
    @GetMapping("/{id}")
    public ApiResult<PromptTemplateVO> getById(@PathVariable Long id) {
        return ApiResult.success(promptManageService.getById(id));
    }

    @SaCheckPermission("system:prompt:view")
    @GetMapping("/{id}/versions")
    public ApiResult<List<PromptTemplateVersionVO>> listVersions(@PathVariable Long id) {
        return ApiResult.success(promptManageService.listVersions(id));
    }

    @SaCheckPermission("system:prompt:edit")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody PromptTemplateDTO dto) {
        return ApiResult.success(promptManageService.create(dto));
    }

    @SaCheckPermission("system:prompt:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PromptTemplateDTO dto) {
        promptManageService.update(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("system:prompt:edit")
    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        promptManageService.publish(id);
        return ApiResult.success();
    }

    @SaCheckPermission("system:prompt:edit")
    @PostMapping("/{id}/test")
    public ApiResult<Map<String, String>> test(@PathVariable Long id, @RequestBody PromptTestDTO dto) {
        return ApiResult.success(Map.of("output", promptManageService.test(id, dto)));
    }
}
