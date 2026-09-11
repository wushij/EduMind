package com.edumind.question.controller.bank;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.question.dto.bank.QuestionBankAddQuestionsDTO;
import com.edumind.question.dto.bank.QuestionBankCreateDTO;
import com.edumind.question.dto.bank.QuestionBankUpdateDTO;
import com.edumind.question.service.bank.QuestionBankService;
import com.edumind.question.vo.bank.QuestionBankVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/question-banks")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @SaCheckPermission("question:view")
    @GetMapping
    public ApiResult<PageResult<QuestionBankVO>> pageQuery(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return ApiResult.success(questionBankService.pageQuery(courseId, keyword, page, pageSize));
    }

    @SaCheckPermission("question:view")
    @GetMapping("/{id}")
    public ApiResult<QuestionBankVO> getById(@PathVariable("id") Long id,
                                             @RequestParam(value = "includeQuestions", defaultValue = "true") boolean includeQuestions) {
        return ApiResult.success(questionBankService.getById(id, includeQuestions));
    }

    @SaCheckPermission("question:edit")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody QuestionBankCreateDTO dto) {
        return ApiResult.success(questionBankService.create(dto));
    }

    @SaCheckPermission("question:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody QuestionBankUpdateDTO dto) {
        questionBankService.update(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("question:edit")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        questionBankService.delete(id);
    }

    @SaCheckPermission("question:edit")
    @PostMapping("/{id}/questions")
    public ApiResult<Void> addQuestions(@PathVariable("id") Long id,
                                        @Valid @RequestBody QuestionBankAddQuestionsDTO dto) {
        questionBankService.addQuestions(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("question:edit")
    @DeleteMapping("/{id}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestion(@PathVariable("id") Long id,
                               @PathVariable("questionId") Long questionId) {
        questionBankService.removeQuestion(id, questionId);
    }
}
