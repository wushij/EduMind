package com.edumind.question.controller.question;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.dto.question.QuestionQueryDTO;
import com.edumind.question.dto.question.QuestionUpdateDTO;
import com.edumind.question.service.question.QuestionService;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @SaCheckPermission("question:view")
    @GetMapping
    public ApiResult<PageResult<QuestionVO>> pageQuery(QuestionQueryDTO query) {
        return ApiResult.success(questionService.pageQuery(query));
    }

    @SaCheckPermission("question:edit")
    @PostMapping
    public ApiResult<Long> createQuestion(@Valid @RequestBody QuestionCreateDTO dto) {
        return ApiResult.success(questionService.createQuestion(dto));
    }

    @SaCheckPermission("question:edit")
    @PostMapping("/batch")
    public ApiResult<QuestionBatchSaveVO> batchSave(@Valid @RequestBody QuestionBatchCreateDTO dto) {
        return ApiResult.success(questionService.batchSave(dto));
    }

    @SaCheckPermission("question:view")
    @GetMapping("/{id}")
    public ApiResult<QuestionVO> getQuestionById(@PathVariable("id") Long id) {
        return ApiResult.success(questionService.getQuestionById(id));
    }

    @SaCheckPermission("question:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> updateQuestion(@PathVariable("id") Long id,
                                          @Valid @RequestBody QuestionUpdateDTO dto) {
        questionService.updateQuestion(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("question:edit")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable("id") Long id) {
        questionService.deleteQuestion(id);
    }
}
