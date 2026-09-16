package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.service.analytics.TeachingAdviceService;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/teaching-advice")
@RequiredArgsConstructor
public class TeachingAdviceController {

    private final TeachingAdviceService teachingAdviceService;

    @SaCheckPermission("course:view")
    @PostMapping
    public ApiResult<TeachingAdviceVO> generateAdvice(@RequestBody TeachingAdviceRequestDTO request) {
        return ApiResult.success(teachingAdviceService.generateAdvice(request));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/latest")
    public ApiResult<TeachingAdviceVO> getLatestAdvice(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        return ApiResult.success(teachingAdviceService.getLatestAdvice(courseId, studentId));
    }

    @SaCheckPermission("course:view")
    @DeleteMapping
    public ApiResult<Void> clearAdvice(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        teachingAdviceService.clearAdvice(courseId, studentId);
        return ApiResult.success();
    }
}
