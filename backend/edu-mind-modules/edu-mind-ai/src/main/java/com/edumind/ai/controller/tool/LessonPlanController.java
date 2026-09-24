package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.ai.dto.tool.LessonPlanDTO;
import com.edumind.ai.service.tool.LessonPlanService;
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
@RequestMapping("/api/ai/lesson-plan")
@RequiredArgsConstructor
public class LessonPlanController {

    private final LessonPlanService lessonPlanService;

    @SaCheckPermission(value = {"ai:lesson:generate", "course:edit", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping
    public ApiResult<Map<String, String>> generate(@Valid @RequestBody LessonPlanDTO dto) {
        return ApiResult.success(Map.of("content", lessonPlanService.generate(dto)));
    }

    @SaCheckPermission(value = {"ai:lesson:generate", "course:edit", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerate(@Valid @RequestBody LessonPlanDTO dto) {
        return lessonPlanService.streamGenerate(dto);
    }
}
