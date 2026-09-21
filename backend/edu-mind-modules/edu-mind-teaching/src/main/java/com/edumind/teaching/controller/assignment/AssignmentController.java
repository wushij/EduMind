package com.edumind.teaching.controller.assignment;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.service.submission.SubmissionService;
import com.edumind.teaching.vo.assignment.AssignmentPaperVO;
import com.edumind.teaching.vo.assignment.AssignmentStatsVO;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.assignment.StudentAssignmentVO;
import com.edumind.teaching.vo.submission.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @SaCheckPermission("assignment:view")
    @GetMapping
    public ApiResult<PageResult<AssignmentVO>> pageQuery(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return ApiResult.success(assignmentService.pageQuery(courseId, status, keyword, page, pageSize));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/stats")
    public ApiResult<AssignmentStatsVO> stats(
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return ApiResult.success(assignmentService.getStats(courseId));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/mine")
    public ApiResult<List<StudentAssignmentVO>> listMine(
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return ApiResult.success(assignmentService.listMine(courseId));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}")
    public ApiResult<AssignmentVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(assignmentService.getById(id));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}/paper")
    public ApiResult<AssignmentPaperVO> getPaper(@PathVariable("id") Long id) {
        return ApiResult.success(assignmentService.getPaper(id));
    }

    @SaCheckPermission("assignment:create")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody AssignmentCreateDTO dto) {
        return ApiResult.success(assignmentService.create(dto));
    }

    @SaCheckPermission("assignment:create")
    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable("id") Long id) {
        assignmentService.publish(id);
        return ApiResult.success();
    }

    @SaCheckPermission("assignment:create")
    @PostMapping("/{id}/close")
    public ApiResult<Void> close(@PathVariable("id") Long id) {
        assignmentService.close(id);
        return ApiResult.success();
    }

    @SaCheckPermission("assignment:create")
    @PostMapping("/{id}/remind")
    public ApiResult<Map<String, Integer>> remind(@PathVariable("id") Long id) {
        int count = assignmentService.remindUnsubmitted(id);
        return ApiResult.success(Map.of("remindedCount", count));
    }

    @SaCheckPermission("assignment:delete")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        assignmentService.delete(id);
    }

    @SaCheckPermission("assignment:view")
    @PostMapping("/{id}/submit")
    public ApiResult<SubmissionVO> submit(@PathVariable("id") Long assignmentId,
                                          @Valid @RequestBody SubmissionCreateDTO dto) {
        return ApiResult.success(submissionService.submit(assignmentId, dto));
    }

    /** 某作业下全部学生答卷，属批改场景，仅教师/管理员可读 */
    @SaCheckPermission("assignment:grade")
    @GetMapping("/{id}/submissions")
    public ApiResult<List<SubmissionVO>> listSubmissions(@PathVariable("id") Long assignmentId) {
        return ApiResult.success(submissionService.listByAssignmentId(assignmentId));
    }
}
