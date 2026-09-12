package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.service.analytics.TeachingAdviceService;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
