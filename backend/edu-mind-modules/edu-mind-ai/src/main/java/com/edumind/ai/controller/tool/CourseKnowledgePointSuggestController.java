package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestDTO;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestResultVO;
import com.edumind.ai.service.tool.CourseKnowledgePointSuggestService;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/course-knowledge-points")
@RequiredArgsConstructor
public class CourseKnowledgePointSuggestController {

    private final CourseKnowledgePointSuggestService suggestService;

    @SaCheckPermission("course:edit")
    @PostMapping("/suggest")
    public ApiResult<CourseKnowledgePointSuggestResultVO> suggest(
            @Valid @RequestBody CourseKnowledgePointSuggestDTO dto) {
        return ApiResult.success(suggestService.suggest(dto));
    }
}
