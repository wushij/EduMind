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
}

