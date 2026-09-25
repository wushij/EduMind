package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/knowledge-mastery")
@RequiredArgsConstructor
public class KnowledgeMasteryController {

    private final KnowledgeMasteryService knowledgeMasteryService;

    /**
     * 课程掌握度画像。
     *
     * <p>注意：{@code studentId} 只表示「聚焦观察的学员」，为空即全班口径。
     * 这里刻意不再用 {@code UserContext.getUserId()} 兜底——教师访问全班视图时，
     * 隐式把自己当成学生会把班级均分与薄弱考点榜全部算错。</p>
     *
     * @param includeTesting 是否把管理员/测试账号计入统计，默认 false
     */
    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<KnowledgeMasteryVO> getKnowledgeMastery(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Boolean includeTesting) {
        return ApiResult.success(
                knowledgeMasteryService.getMastery(courseId, studentId, Boolean.TRUE.equals(includeTesting)));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/heatmap")
    public ApiResult<java.util.Map<String, Object>> getMasteryHeatmap(
            @RequestParam Long courseId,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Boolean includeTesting) {
        return ApiResult.success(
                knowledgeMasteryService.getHeatmap(courseId, range, Boolean.TRUE.equals(includeTesting)));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/heatmap/cell")
    public ApiResult<java.util.Map<String, Object>> getHeatmapCell(
            @RequestParam Long courseId,
            @RequestParam Long studentId,
            @RequestParam Long knowledgePointId) {
        return ApiResult.success(knowledgeMasteryService.getHeatmapCell(courseId, studentId, knowledgePointId));
    }
}
