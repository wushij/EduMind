package com.edumind.course.controller.lesson;

import com.edumind.common.api.ApiResult;
import com.edumind.course.dto.lesson.LessonProgressUpdateDTO;
import com.edumind.course.dto.lesson.LessonUpdateDTO;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.lesson.LessonService;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;
import com.edumind.course.vo.lesson.LessonDetailVO;
import com.edumind.course.vo.lesson.LessonProgressVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final CourseAccessService courseAccessService;

    @SaCheckPermission("course:view")
    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ApiResult<LessonDetailVO> getLesson(@PathVariable Long courseId,
                                               @PathVariable Long lessonId,
                                               @RequestParam(value = "preview", defaultValue = "false") boolean preview) {
        boolean canPreview = false;
        try {
            var course = courseAccessService.requireCourse(courseId);
            canPreview = courseAccessService.canEdit(course);
        } catch (Exception ignored) {
            canPreview = false;
        }
        return ApiResult.success(lessonService.getLessonDetail(courseId, lessonId, preview && canPreview));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/{courseId}/lessons/{lessonId}")
    public ApiResult<Void> updateLesson(@PathVariable Long courseId,
                                        @PathVariable Long lessonId,
                                        @Valid @RequestBody LessonUpdateDTO dto) {
        lessonService.updateLesson(courseId, lessonId, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/{courseId}/lessons/{lessonId}/publish")
    public ApiResult<Void> publishLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        lessonService.publishLesson(courseId, lessonId);
        return ApiResult.success();
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/{courseId}/lessons/{lessonId}/unpublish")
    public ApiResult<Void> unpublishLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        lessonService.unpublishLesson(courseId, lessonId);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{courseId}/lessons/{lessonId}/progress")
    public ApiResult<LessonProgressVO> getProgress(@PathVariable Long courseId, @PathVariable Long lessonId) {
        return ApiResult.success(lessonService.getProgress(courseId, lessonId));
    }

    @SaCheckPermission("course:view")
    @PutMapping("/{courseId}/lessons/{lessonId}/progress")
    public ApiResult<LessonProgressVO> updateProgress(@PathVariable Long courseId,
                                                      @PathVariable Long lessonId,
                                                      @RequestBody LessonProgressUpdateDTO dto) {
        return ApiResult.success(lessonService.updateProgress(courseId, lessonId, dto));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{courseId}/progress/summary")
    public ApiResult<CourseLessonProgressSummaryVO> progressSummary(@PathVariable Long courseId) {
        return ApiResult.success(lessonService.getProgressSummary(courseId));
    }
}
