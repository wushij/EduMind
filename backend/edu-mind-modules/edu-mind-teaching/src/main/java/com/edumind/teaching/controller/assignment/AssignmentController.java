package com.edumind.teaching.controller.assignment;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.service.submission.SubmissionService;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.submission.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return ApiResult.success(assignmentService.pageQuery(courseId, status, page, pageSize));
    }

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}")
    public ApiResult<AssignmentVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(assignmentService.getById(id));
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

    @SaCheckPermission("assignment:view")
    @GetMapping("/{id}/submissions")
    public ApiResult<List<SubmissionVO>> listSubmissions(@PathVariable("id") Long assignmentId) {
        return ApiResult.success(submissionService.listByAssignmentId(assignmentId));
    }
}
