package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.WrongQuestionAnalyticsService;
import com.edumind.statistics.service.analytics.WrongQuestionDiagnosisService;
import com.edumind.statistics.vo.analytics.WrongQuestionAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionAnalyticsService wrongQuestionAnalyticsService;
    private final WrongQuestionDiagnosisService wrongQuestionDiagnosisService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<WrongQuestionAnalyticsVO> listWrongQuestions(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.success(wrongQuestionAnalyticsService.listWrongQuestions(courseId, knowledgePointId, page, pageSize));
    }

    @SaCheckPermission("course:view")
    @PostMapping("/{id}/diagnose")
    public ApiResult<WrongQuestionAnalyticsVO.WrongQuestionItemVO> diagnose(@PathVariable("id") Long id) {
        WrongQuestionRecordEntity entity = wrongQuestionDiagnosisService.diagnoseRecord(id);
        WrongQuestionAnalyticsVO.WrongQuestionItemVO vo = new WrongQuestionAnalyticsVO.WrongQuestionItemVO();
        vo.setId(entity.getId());
        vo.setQuestionId(entity.getQuestionId());
        vo.setWrongCount(entity.getWrongCount());
        vo.setDiagnosis(entity.getDiagnosis());
        if (entity.getErrorTypes() != null) {
            vo.setErrorTypes(java.util.Arrays.asList(entity.getErrorTypes().split(",")));
        }
        if (entity.getVariantQuestionIds() != null && !entity.getVariantQuestionIds().isBlank()) {
            vo.setVariantQuestionIds(java.util.Arrays.stream(entity.getVariantQuestionIds().split(","))
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .collect(java.util.stream.Collectors.toList()));
        }
        return ApiResult.success(vo);
    }

    @SaCheckPermission("course:view")
    @PostMapping("/{id}/cancel-diagnose")
    public ApiResult<Void> cancelDiagnose(@PathVariable("id") Long id) {
        wrongQuestionDiagnosisService.cancelDiagnosis(id);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @PostMapping("/macro-diagnose")
    public ApiResult<java.util.Map<String, String>> diagnoseMacro(@RequestParam Long courseId) {
        WrongQuestionAnalyticsVO analytics = wrongQuestionAnalyticsService.listWrongQuestions(courseId, null, 1, 50);
        int total = analytics != null && analytics.getTotalWrongRecords() != null
                ? analytics.getTotalWrongRecords().intValue() : 0;
        java.util.Map<String, Integer> dist = analytics != null ? analytics.getErrorTypeDistribution() : null;
        java.util.List<String> weakKpNames = analytics != null && analytics.getTopWeakKnowledgePoints() != null
                ? analytics.getTopWeakKnowledgePoints().stream()
                    .map(WrongQuestionAnalyticsVO.WeakKpSummaryVO::getKnowledgePointName)
                    .filter(org.springframework.util.StringUtils::hasText)
                    .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();

        String report = wrongQuestionDiagnosisService.diagnoseClassMacro(courseId, total, dist, weakKpNames);
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("report", report);
        return ApiResult.success(result);
    }

    @SaCheckPermission("course:view")
    @PostMapping("/cancel-macro-diagnose")
    public ApiResult<Void> cancelMacroDiagnose(@RequestParam Long courseId) {
        wrongQuestionDiagnosisService.cancelMacroDiagnosis(courseId);
        return ApiResult.success();
    }
}

