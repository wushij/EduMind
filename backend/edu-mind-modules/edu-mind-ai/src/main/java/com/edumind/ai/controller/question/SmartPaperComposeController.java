package com.edumind.ai.controller.question;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.question.SmartPaperComposeDTO;
import com.edumind.ai.service.question.SmartPaperComposeService;
import com.edumind.common.api.ApiResult;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
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
        int count = dto.getTotalCount() != null ? dto.getTotalCount() : 10;
        return ApiResult.success(smartPaperComposeService.compose(
                dto.getCourseId(), dto.getKnowledgePointIds(), count, dto.getExcludeIds()));
    }
}
