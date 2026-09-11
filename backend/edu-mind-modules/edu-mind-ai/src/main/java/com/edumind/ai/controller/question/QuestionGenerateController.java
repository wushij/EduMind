package com.edumind.ai.controller.question;

import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.service.question.QuestionGenerateService;
import com.edumind.common.api.ApiResult;
import com.edumind.question.vo.question.QuestionVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/questions")
@RequiredArgsConstructor
public class QuestionGenerateController {

    private final QuestionGenerateService questionGenerateService;

    @SaCheckPermission("ai:question")
    @PostMapping("/generate")
    public ApiResult<List<QuestionVO>> generate(@Valid @RequestBody QuestionGenerateDTO dto) {
        return ApiResult.success(questionGenerateService.generate(dto));
    }
}
