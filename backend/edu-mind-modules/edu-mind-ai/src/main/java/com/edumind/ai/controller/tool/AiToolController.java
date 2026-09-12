package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.service.tool.AiToolService;
import com.edumind.ai.vo.AiToolVO;
import com.edumind.common.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/tools")
@RequiredArgsConstructor
public class AiToolController {

    private final AiToolService aiToolService;

    @SaCheckPermission("ai:chat")
    @GetMapping
    public ApiResult<List<AiToolVO>> listTools(
            @RequestParam(value = "category", defaultValue = "ALL") String category,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.success(aiToolService.listTools(category, keyword));
    }

    @SaCheckPermission("ai:chat")
    @PostMapping("/{id}/use")
    public ApiResult<Void> recordToolUse(@PathVariable String id) {
        aiToolService.recordToolUse(id);
        return ApiResult.success();
    }
}
