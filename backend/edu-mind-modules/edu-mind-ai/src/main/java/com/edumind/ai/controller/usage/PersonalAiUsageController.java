package com.edumind.ai.controller.usage;

import com.edumind.ai.service.usage.PersonalAiUsageService;
import com.edumind.ai.vo.usage.PersonalAiUsageVO;
import com.edumind.common.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/ai-usage")
@RequiredArgsConstructor
public class PersonalAiUsageController {

    private final PersonalAiUsageService personalAiUsageService;

    @GetMapping
    public ApiResult<PersonalAiUsageVO> getMyUsage(
            @RequestParam(defaultValue = "7") int logDays,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResult.success(personalAiUsageService.getMyUsage(logDays, pageNum, pageSize));
    }
}
