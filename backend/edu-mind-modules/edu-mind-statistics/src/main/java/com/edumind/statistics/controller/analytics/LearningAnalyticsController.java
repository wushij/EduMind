package com.edumind.statistics.controller.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/learning")
@RequiredArgsConstructor
public class LearningAnalyticsController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";

    private final LearningAnalyticsService learningAnalyticsService;

    /**
     * 班级整体学情分析。
     * 这是典型的教师/管理侧数据（含全班学号、成绩与对比），学生不得访问，
     * 因此这里不再只校验 course:view（学生同样具备该权限），而是显式限定角色，
     * 避免学生通过 URL 直接拿到全班数据。
     */
    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<LearningAnalyticsVO> getLearningAnalytics(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) Long classId) {
        if (!isTeacherOrAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "仅教师或管理员可以查看班级整体学情分析");
        }
        return ApiResult.success(learningAnalyticsService.getLearningAnalytics(courseId, range, classId));
    }

    /**
     * 学生个体学情画像。
     * 教师/管理员可查看任意学员；学生只能查看本人——
     * 传入的 studentId 会被忽略并强制指向当前登录用户，防止学生构造参数窥探同学画像。
     */
    @SaCheckPermission("course:view")
    @GetMapping("/portrait")
    public ApiResult<StudentPortraitVO> getStudentPortrait(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "30d") String range) {
        Long targetStudent = isTeacherOrAdmin() ? (studentId != null ? studentId : UserContext.getUserId()) : UserContext.getUserId();
        if (targetStudent == null) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无法确定诊断学员，请重新登录后再试");
        }
        try {
            return ApiResult.success(learningAnalyticsService.getStudentPortrait(courseId, targetStudent, range));
        } catch (BusinessException e) {
            // 管理员/教师在自适应练习或平台巡检体验时，若本人并非该课修读学生，兜底返回空画像，避免阻塞业务
            if (isTeacherOrAdmin() && studentId == null) {
                return ApiResult.success(new StudentPortraitVO());
            }
            throw e;
        }
    }

    private boolean isTeacherOrAdmin() {
        LoginUser user = UserContext.get();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(ROLE_ADMIN) || user.getRoles().contains(ROLE_TEACHER);
    }
}
