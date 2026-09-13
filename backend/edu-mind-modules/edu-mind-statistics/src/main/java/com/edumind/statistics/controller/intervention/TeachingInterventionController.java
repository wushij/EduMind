package com.edumind.statistics.controller.intervention;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教学干预建议与决策控制器 (严格遵守 Controller -> DTO -> Service -> DAO -> Mapper -> Entity 架构规范)
 */
@RestController
@RequestMapping("/api/analytics/interventions")
@RequiredArgsConstructor
@SaCheckLogin
public class TeachingInterventionController {

    private final TeachingInterventionService teachingInterventionService;

    @GetMapping
    public ApiResult<List<TeachingInterventionVO>> listInterventions(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(teachingInterventionService.listInterventions(courseId));
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Void> approveIntervention(@PathVariable("id") Long id, @RequestBody(required = false) InterventionActionDTO dto) {
        teachingInterventionService.approveIntervention(id, dto);
        return ApiResult.success();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> rejectIntervention(@PathVariable("id") Long id) {
        teachingInterventionService.rejectIntervention(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/dispatch")
    public ApiResult<Void> dispatchIntervention(@PathVariable("id") Long id) {
        teachingInterventionService.dispatchIntervention(id);
        return ApiResult.success();
    }
}
