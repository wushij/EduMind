package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/knowledge-mastery")
@RequiredArgsConstructor
public class KnowledgeMasteryController {

    private final KnowledgeMasteryService knowledgeMasteryService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<KnowledgeMasteryVO> getKnowledgeMastery(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        Long targetStudent = studentId != null ? studentId : UserContext.getUserId();
        return ApiResult.success(knowledgeMasteryService.getMastery(courseId, targetStudent));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/heatmap")
    public ApiResult<java.util.Map<String, Object>> getMasteryHeatmap(
            @RequestParam Long courseId,
            @RequestParam(required = false) String range) {
        return ApiResult.success(knowledgeMasteryService.getHeatmap(courseId, range));
    }
}
