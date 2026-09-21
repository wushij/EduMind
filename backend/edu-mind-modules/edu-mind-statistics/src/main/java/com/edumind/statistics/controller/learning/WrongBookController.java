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

import java.util.List;

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

    /** 按需生成同构变式题（大模型生成 + 落库），与诊断解耦，避免一次请求串行多次模型调用 */
    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/variants")
    public ApiResult<List<WrongBookDetailVO.VariantQuestionSummaryVO>> generateVariants(
            @PathVariable("id") Long id,
            @RequestParam(name = "regenerate", defaultValue = "false") boolean regenerate) {
        Long studentId = LoginUserResolver.requireUserId();
        return ApiResult.success(wrongBookService.generateVariants(studentId, id, regenerate));
    }

    /**
     * 学生中止 AI 归因诊断。
     * 浏览器关闭连接不会中断服务端线程，本次模型调用无法撤销；
     * 该接口让服务端在模型返回后丢弃结果、不再落库，避免"点了中止却还是冒出新结论"。
     */
    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/diagnose/cancel")
    public ApiResult<Void> cancelDiagnose(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        wrongBookService.cancelAiDiagnosis(studentId, id);
        return ApiResult.success(null);
    }

    /** 学生中止变式题生成：服务端丢弃结果，且不把题目写入题库 */
    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/variants/cancel")
    public ApiResult<Void> cancelVariants(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        wrongBookService.cancelAiVariants(studentId, id);
        return ApiResult.success(null);
    }

    @SaCheckPermission("learning:wrong:view")
    @PostMapping("/{id}/master")
    public ApiResult<Void> markMastered(@PathVariable("id") Long id) {
        Long studentId = LoginUserResolver.requireUserId();
        wrongBookService.markMastered(studentId, id);
        return ApiResult.success(null);
    }
}
