package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.ai.dto.tool.CourseDescriptionSuggestDTO;
import com.edumind.ai.dto.tool.CourseDescriptionSuggestResultVO;
import com.edumind.ai.service.tool.CourseDescriptionSuggestService;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/course-profile")
@RequiredArgsConstructor
public class CourseDescriptionSuggestController {

    private final CourseDescriptionSuggestService descriptionSuggestService;

    @SaCheckPermission(value = {"course:edit", "course:create", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping("/suggest-description")
    public ApiResult<CourseDescriptionSuggestResultVO> suggestDescription(
            @Valid @RequestBody CourseDescriptionSuggestDTO dto) {
        return ApiResult.success(descriptionSuggestService.suggest(dto));
    }
}
