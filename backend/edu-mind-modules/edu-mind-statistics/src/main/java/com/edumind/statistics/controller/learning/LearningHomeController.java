package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.statistics.service.learning.LearningHomeService;
import com.edumind.statistics.vo.learning.LearningHomeOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/home")
@RequiredArgsConstructor
public class LearningHomeController {

    private final LearningHomeService learningHomeService;

    @SaCheckPermission("course:view")
    @GetMapping("/overview")
    public ApiResult<LearningHomeOverviewVO> overview(
            @RequestParam(required = false) Long primaryCourseId) {
        Long studentId = UserContext.getUserId();
        return ApiResult.success(learningHomeService.getOverview(studentId, primaryCourseId));
    }
}
