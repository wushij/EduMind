package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.service.learning.WrongBookService;
import com.edumind.statistics.vo.learning.WrongBookDetailVO;
import com.edumind.statistics.vo.learning.WrongBookItemVO;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/wrong-book")
@RequiredArgsConstructor
public class WrongBookController {

    private final WrongBookService wrongBookService;

    @SaCheckPermission("learning:wrong:view")
    @GetMapping
    public ApiResult<WrongBookListVO> list(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String errorType,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) Integer status) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(wrongBookService.list(
                studentId, courseId, page, pageSize, errorType, knowledgePointId, status));
    }

    @SaCheckPermission("learning:wrong:view")
    @GetMapping("/overview")
    public ApiResult<WrongBookOverviewVO> overview(@RequestParam Long courseId) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(wrongBookService.overview(studentId, courseId));
    }

    @SaCheckPermission("learning:wrong:view")
    @GetMapping("/{id}")
    public ApiResult<WrongBookDetailVO> detail(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(wrongBookService.detail(studentId, id));
    }

    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/diagnose")
    public ApiResult<WrongBookItemVO> diagnose(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(wrongBookService.diagnose(studentId, id));
    }

    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/master")
    public ApiResult<Void> markMastered(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        wrongBookService.markMastered(studentId, id);
        return ApiResult.success(null);
    }
}
