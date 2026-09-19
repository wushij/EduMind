package com.edumind.statistics.controller.learning;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.statistics.service.learning.AdaptivePathService;
import com.edumind.statistics.vo.learning.LearningPathDetailVO;
import com.edumind.statistics.vo.learning.LearningPathStudentItemVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningPathController {

    private final AdaptivePathService adaptivePathService;
    private final CourseAccessApi courseAccessApi;

    @SaCheckPermission("learning:path:view")
    @GetMapping("/adaptive-path")
    public ApiResult<LearningPathVO> adaptivePath(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        Long targetStudent = resolveTargetStudent(courseId, studentId);
        return ApiResult.success(adaptivePathService.buildAdaptivePath(courseId, targetStudent));
    }

    @SaCheckPermission("learning:path:view")
    @GetMapping("/adaptive-path/detail")
    public ApiResult<LearningPathDetailVO> adaptivePathDetail(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId) {
        Long targetStudent = resolveTargetStudent(courseId, studentId);
        return ApiResult.success(adaptivePathService.buildDetail(courseId, targetStudent));
    }

    @SaCheckPermission("learning:path:view")
    @GetMapping("/adaptive-path/students")
    public ApiResult<List<LearningPathStudentItemVO>> listStudents(@RequestParam Long courseId) {
        courseAccessApi.assertCanView(courseId);
        return ApiResult.success(adaptivePathService.listCourseStudents(courseId));
    }

    private Long resolveTargetStudent(Long courseId, Long studentId) {
        Long currentUserId = UserContext.getUserId();
        if (studentId == null || studentId.equals(currentUserId)) {
            return currentUserId;
        }
        courseAccessApi.assertCanView(courseId);
        return studentId;
    }
}
