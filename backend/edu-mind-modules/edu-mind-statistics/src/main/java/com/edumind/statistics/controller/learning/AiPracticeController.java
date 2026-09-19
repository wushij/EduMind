package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.dto.learning.AiPracticeGradeDTO;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.dto.learning.AiPracticeSubmitDTO;
import com.edumind.statistics.service.learning.AIPracticeService;
import com.edumind.statistics.vo.learning.AiPracticeGradeVO;
import com.edumind.statistics.vo.learning.AiPracticeSessionVO;
import com.edumind.statistics.vo.learning.AiPracticeSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class AiPracticeController {

    private final AIPracticeService aiPracticeService;

    @SaCheckPermission("course:view")
    @PostMapping("/ai-practice/start")
    public ApiResult<AiPracticeSessionVO> startPractice(@Valid @RequestBody AiPracticeStartDTO dto) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.startPractice(studentId, dto));
    }

    @SaCheckPermission("course:view")
    @PostMapping("/ai-practice/grade")
    public ApiResult<AiPracticeGradeVO> gradeAnswer(@Valid @RequestBody AiPracticeGradeDTO dto) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.gradeAnswer(studentId, dto));
    }

    @SaCheckPermission("course:view")
    @PostMapping("/ai-practice/submit")
    public ApiResult<AiPracticeSubmitVO> submitPractice(@Valid @RequestBody AiPracticeSubmitDTO dto) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.submitPractice(studentId, dto));
    }
}
