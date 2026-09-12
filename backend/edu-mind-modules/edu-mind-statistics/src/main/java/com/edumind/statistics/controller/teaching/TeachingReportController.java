package com.edumind.statistics.controller.teaching;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.teaching.TeachingReportService;
import com.edumind.statistics.vo.teaching.TeachingReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/teaching-report")
@RequiredArgsConstructor
public class TeachingReportController {

    private final TeachingReportService teachingReportService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<TeachingReportVO> getTeachingReport(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "7d") String range) {
        return ApiResult.success(teachingReportService.buildReport(courseId, range));
    }
}
