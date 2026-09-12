package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.learning.LearningPathService;
import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<LearningPathVO> getLearningPath(@RequestParam Long courseId) {
        return ApiResult.success(learningPathService.buildPath(courseId));
    }
}
