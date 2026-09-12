package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @SaCheckPermission("course:view")
    @GetMapping("/questions")
    public ApiResult<List<RecommendedQuestionVO>> recommendQuestions(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "chapterId", required = false) Long chapterId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResult.success(recommendationService.recommendQuestions(courseId, chapterId, limit));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/resources")
    public ApiResult<List<RecommendedResourceVO>> recommendResources(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "chapterId", required = false) Long chapterId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResult.success(recommendationService.recommendResources(courseId, chapterId, limit));
    }
}
