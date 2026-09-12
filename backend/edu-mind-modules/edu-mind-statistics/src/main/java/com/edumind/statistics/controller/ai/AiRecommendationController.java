package com.edumind.statistics.controller.ai;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final RecommendationService recommendationService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<Map<String, Object>> recommend(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(defaultValue = "8") Integer limit) {
        List<RecommendedQuestionVO> questions = recommendationService.recommendQuestions(courseId, chapterId, limit);
        List<RecommendedResourceVO> resources = recommendationService.recommendResources(courseId, chapterId, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("questions", questions);
        data.put("resources", resources);
        return ApiResult.success(data);
    }
}
