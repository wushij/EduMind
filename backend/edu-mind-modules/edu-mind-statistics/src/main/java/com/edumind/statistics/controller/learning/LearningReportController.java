package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.statistics.service.learning.LearningReportService;
import com.edumind.statistics.vo.learning.LearningReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/report")
@RequiredArgsConstructor
public class LearningReportController {

    private final LearningReportService learningReportService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<LearningReportVO> getReport(
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "30d") String range) {
        Long studentId = UserContext.getUserId();
        return ApiResult.success(learningReportService.getReport(studentId, courseId, range));
    }
}
