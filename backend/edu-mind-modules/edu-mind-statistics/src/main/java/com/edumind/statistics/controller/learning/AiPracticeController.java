package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.dto.learning.AiPracticeSubmitDTO;
import com.edumind.statistics.service.learning.AIPracticeService;
import com.edumind.statistics.vo.learning.AiPracticeSessionVO;
import com.edumind.statistics.vo.learning.AiPracticeSubmitVO;
import com.edumind.statistics.vo.learning.StudentWrongQuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class AiPracticeController {

    private final AIPracticeService aiPracticeService;

    @SaCheckPermission("course:view")
    @GetMapping("/wrong-book")
    public ApiResult<StudentWrongQuestionVO> listWrongBook(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.listWrongQuestions(studentId, courseId, page, pageSize));
    }

    @SaCheckPermission("course:view")
    @PostMapping("/ai-practice/start")
    public ApiResult<AiPracticeSessionVO> startPractice(@Valid @RequestBody AiPracticeStartDTO dto) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.startPractice(studentId, dto));
    }

    @SaCheckPermission("course:view")
    @PostMapping("/ai-practice/submit")
    public ApiResult<AiPracticeSubmitVO> submitPractice(@Valid @RequestBody AiPracticeSubmitDTO dto) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(aiPracticeService.submitPractice(studentId, dto));
    }
}
