package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestDTO;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestResultVO;
import com.edumind.ai.service.tool.CourseObjectiveSuggestService;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/course-objectives")
@RequiredArgsConstructor
public class CourseObjectiveSuggestController {

    private final CourseObjectiveSuggestService suggestService;

    @SaCheckPermission(value = {"course:edit", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping("/suggest")
    public ApiResult<CourseObjectiveSuggestResultVO> suggest(@Valid @RequestBody CourseObjectiveSuggestDTO dto) {
        return ApiResult.success(suggestService.suggest(dto));
    }
}
