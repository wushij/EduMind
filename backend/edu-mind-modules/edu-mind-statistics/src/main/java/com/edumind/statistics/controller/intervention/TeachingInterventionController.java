package com.edumind.statistics.controller.intervention;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.intervention.InterventionOverviewStatsVO;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @SaCheckPermission("analytics:intervention:view")
    public ApiResult<List<TeachingInterventionVO>> listInterventions(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(teachingInterventionService.listInterventions(courseId));
    }

    @GetMapping("/overview-stats")
    @SaCheckPermission("analytics:intervention:view")
    public ApiResult<InterventionOverviewStatsVO> getOverviewStats(@RequestParam(required = false) Long courseId) {
        return ApiResult.success(teachingInterventionService.getOverviewStats(courseId));
    }

    @PostMapping("/scan-trigger")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "学情诊断巡检与提案生成", businessType = BusinessType.INSERT)
    public ApiResult<TeachingInterventionVO> scanAndGenerateProposal(@RequestParam Long courseId) {
        return ApiResult.success(teachingInterventionService.scanAndGenerateProposal(courseId));
    }

    @PostMapping("/ai-propose")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "AI大模型智能推演干预预案", businessType = BusinessType.OTHER)
    public ApiResult<TeachingInterventionVO> generateAiInterventionProposal(@Valid @RequestBody com.edumind.statistics.dto.intervention.InterventionAiProposeDTO dto) {
        return ApiResult.success(teachingInterventionService.generateAiInterventionProposal(dto));
    }

    @PostMapping
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "创建干预预案", businessType = BusinessType.INSERT)
    public ApiResult<TeachingInterventionVO> createIntervention(@Valid @RequestBody InterventionCreateDTO dto) {
        return ApiResult.success(teachingInterventionService.createIntervention(dto));
    }

    @PutMapping("/{id}/customize")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "自定义调整微课与试题", businessType = BusinessType.UPDATE)
    public ApiResult<Void> customizeIntervention(@PathVariable("id") Long id, @RequestBody InterventionActionDTO dto) {
        teachingInterventionService.customizeIntervention(id, dto);
        return ApiResult.success();
    }

    @PostMapping("/{id}/approve")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "审批通过干预预案", businessType = BusinessType.GRANT)
    public ApiResult<Void> approveIntervention(@PathVariable("id") Long id, @RequestBody(required = false) InterventionActionDTO dto) {
        teachingInterventionService.approveIntervention(id, dto);
        return ApiResult.success();
    }

    @PostMapping("/{id}/reject")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "驳回干预预案", businessType = BusinessType.DELETE)
    public ApiResult<Void> rejectIntervention(@PathVariable("id") Long id) {
        teachingInterventionService.rejectIntervention(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/dispatch")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "派发干预预案", businessType = BusinessType.GRANT)
    public ApiResult<Void> dispatchIntervention(@PathVariable("id") Long id) {
        teachingInterventionService.dispatchIntervention(id);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("analytics:intervention:manage")
    @OperationLog(module = "教学干预", title = "删除干预预案", businessType = BusinessType.DELETE)
    public ApiResult<Void> deleteIntervention(@PathVariable("id") Long id) {
        teachingInterventionService.deleteIntervention(id);
        return ApiResult.success();
    }
}
