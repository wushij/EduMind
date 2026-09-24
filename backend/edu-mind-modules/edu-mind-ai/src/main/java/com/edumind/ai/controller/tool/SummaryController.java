package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.ai.dto.tool.SummaryDTO;
import com.edumind.ai.service.tool.SummaryService;
import com.edumind.common.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping
    public ApiResult<Map<String, String>> summarize(@RequestBody SummaryDTO dto) {
        return ApiResult.success(Map.of("content", summaryService.summarize(dto)));
    }
}
