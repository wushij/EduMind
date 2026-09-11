package com.edumind.teaching.controller.exam;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.exam.ExamCreateDTO;
import com.edumind.teaching.dto.exam.ExamUpdateDTO;
import com.edumind.teaching.service.exam.ExamService;
import com.edumind.teaching.vo.exam.ExamExportVO;
import com.edumind.teaching.vo.exam.ExamVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @SaCheckPermission("exam:view")
    @GetMapping
    public ApiResult<PageResult<ExamVO>> pageQuery(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return ApiResult.success(examService.pageQuery(courseId, keyword, page, pageSize));
    }

    @SaCheckPermission("exam:view")
    @GetMapping("/{id}")
    public ApiResult<ExamVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(examService.getById(id));
    }

    /**
     * 试卷导出预览（V0.2 返回 JSON 包；PDF 导出归后续版本）
     */
    @SaCheckPermission("exam:view")
    @GetMapping("/{id}/export")
    public ApiResult<ExamExportVO> exportExam(@PathVariable("id") Long id) {
        return ApiResult.success(examService.exportExam(id));
    }

    @SaCheckPermission("exam:edit")
    @PostMapping
    public ApiResult<Map<String, Long>> create(@Valid @RequestBody ExamCreateDTO dto) {
        Long examId = examService.create(dto);
        return ApiResult.success(Map.of("examId", examId));
    }

    @SaCheckPermission("exam:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable("id") Long id,
                                  @Valid @RequestBody ExamUpdateDTO dto) {
        examService.update(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("exam:edit")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        examService.delete(id);
    }
}
