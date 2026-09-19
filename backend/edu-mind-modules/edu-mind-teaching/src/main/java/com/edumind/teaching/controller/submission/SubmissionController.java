package com.edumind.teaching.controller.submission;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.submission.GradingReviewDTO;
import com.edumind.teaching.dto.submission.SubmissionBatchGradeDTO;
import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.service.grading.GradingService;
import com.edumind.teaching.service.submission.SubmissionOverviewService;
import com.edumind.teaching.service.submission.SubmissionService;
import com.edumind.teaching.vo.submission.GradingItemVO;
import com.edumind.teaching.vo.submission.SubmissionListItemVO;
import com.edumind.teaching.vo.submission.SubmissionOverviewStatsVO;
import com.edumind.teaching.vo.submission.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradingService gradingService;
    private final SubmissionOverviewService submissionOverviewService;

    @SaCheckPermission("assignment:view")
    @GetMapping
    public ApiResult<PageResult<SubmissionListItemVO>> pageQuery(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return ApiResult.success(submissionOverviewService.pageQuery(
                courseId, assignmentId, status, keyword, page, pageSize));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/stats")
    public ApiResult<SubmissionOverviewStatsVO> stats(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        return ApiResult.success(submissionOverviewService.getStats(courseId, assignmentId));
    }

    @SaCheckPermission("ai:grading")
    @PostMapping("/batch-grade")
    public ApiResult<Map<String, Integer>> batchGrade(@RequestBody SubmissionBatchGradeDTO dto) {
        int count = submissionOverviewService.batchGrade(dto);
        return ApiResult.success(Map.of("successCount", count));
    }

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
