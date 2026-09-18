package com.edumind.ai.controller.teaching;

import com.edumind.ai.dto.teaching.LessonContentGenerateDTO;
import com.edumind.ai.service.teaching.LessonContentGenerateService;
import com.edumind.common.api.ApiResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/lesson-content")
@RequiredArgsConstructor
public class LessonContentController {

    private final LessonContentGenerateService lessonContentGenerateService;

    @SaCheckPermission("course:edit")
    @PostMapping("/generate")
    public ApiResult<Map<String, String>> generate(@Valid @RequestBody LessonContentGenerateDTO dto) {
        String contentJson = lessonContentGenerateService.generateAndSaveDraft(dto);
        return ApiResult.success(Map.of("contentJson", contentJson));
    }
}
