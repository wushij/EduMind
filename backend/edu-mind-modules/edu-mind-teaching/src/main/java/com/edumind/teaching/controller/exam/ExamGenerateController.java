package com.edumind.teaching.controller.exam;

import com.edumind.common.api.ApiResult;
import com.edumind.teaching.dto.exam.ExamGenerateDTO;
import com.edumind.teaching.service.exam.ExamGenerateService;
import com.edumind.teaching.vo.exam.ExamPreviewVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/exams")
@RequiredArgsConstructor
public class ExamGenerateController {

    private final ExamGenerateService examGenerateService;

    @SaCheckPermission("ai:exam")
    @PostMapping("/generate")
    public ApiResult<ExamPreviewVO> generate(@Valid @RequestBody ExamGenerateDTO dto) {
        return ApiResult.success(examGenerateService.generate(dto));
    }
}
