package com.edumind.teaching.controller.submission;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.teaching.dto.submission.GradingReviewDTO;
import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.service.grading.GradingService;
import com.edumind.teaching.service.submission.SubmissionService;
import com.edumind.teaching.vo.submission.GradingItemVO;
import com.edumind.teaching.vo.submission.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradingService gradingService;

    @SaCheckPermission("assignment:view")
    @PostMapping("/assignments/{assignmentId}")
    public ApiResult<SubmissionVO> submit(@PathVariable("assignmentId") Long assignmentId,
                                          @Valid @RequestBody SubmissionCreateDTO dto) {
        return ApiResult.success(submissionService.submit(assignmentId, dto));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}")
    public ApiResult<SubmissionVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(submissionService.getById(id));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}/grading")
    public ApiResult<List<GradingItemVO>> getGradingResults(@PathVariable("id") Long id) {
        return ApiResult.success(gradingService.getGradingResults(id));
    }

    @SaCheckPermission("ai:grading")
    @PostMapping("/{id}/grade")
    public ApiResult<Void> triggerGrade(@PathVariable("id") Long id) {
        gradingService.gradeSubmission(id);
        return ApiResult.success();
    }

    @SaCheckPermission("ai:grading")
    @PutMapping("/{id}/grading/review")
    public ApiResult<Void> reviewGrading(@PathVariable("id") Long id,
                                         @Valid @RequestBody GradingReviewDTO dto) {
        gradingService.reviewGrading(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/assignments/{assignmentId}")
    public ApiResult<List<SubmissionVO>> listByAssignment(@PathVariable("assignmentId") Long assignmentId) {
        return ApiResult.success(submissionService.listByAssignmentId(assignmentId));
    }
}
