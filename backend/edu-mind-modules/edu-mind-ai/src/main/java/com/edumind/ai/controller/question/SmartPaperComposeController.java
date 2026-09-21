package com.edumind.ai.controller.question;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.question.SmartPaperComposeDTO;
import com.edumind.ai.dto.question.SmartPaperSwapDTO;
import com.edumind.ai.service.question.SmartPaperComposeService;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.common.api.ApiResult;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/paper")
@RequiredArgsConstructor
public class SmartPaperComposeController {

    private final SmartPaperComposeService smartPaperComposeService;

    @SaCheckPermission("ai:exam")
    @PostMapping("/compose")
    public ApiResult<SmartPaperComposeVO> compose(@RequestBody SmartPaperComposeDTO dto) {
        return ApiResult.success(smartPaperComposeService.composeV2(dto));
    }

    @SaCheckPermission("ai:exam")
    @PostMapping("/compose/cancel")
    public ApiResult<Void> cancelCompose() {
        smartPaperComposeService.cancelActiveGeneration();
        return ApiResult.success();
    }

    @SaCheckPermission("ai:exam")
    @PostMapping("/swap-question")
    public ApiResult<QuestionVO> swapQuestion(@RequestBody SmartPaperSwapDTO dto) {
        return ApiResult.success(smartPaperComposeService.swapQuestion(dto));
    }
}
