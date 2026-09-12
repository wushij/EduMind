package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.statistics.service.learning.AdaptivePathService;
import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningPathController {

    private final AdaptivePathService adaptivePathService;

    @SaCheckPermission("course:view")
    @GetMapping("/adaptive-path")
    public ApiResult<LearningPathVO> adaptivePath(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        Long targetStudent = studentId != null ? studentId : UserContext.getUserId();
        return ApiResult.success(adaptivePathService.buildAdaptivePath(courseId, targetStudent));
    }
}
