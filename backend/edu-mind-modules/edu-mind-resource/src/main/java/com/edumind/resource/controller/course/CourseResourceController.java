package com.edumind.resource.controller.course;

import com.edumind.common.api.ApiResult;
import com.edumind.resource.service.course.CourseResourceService;
import com.edumind.resource.vo.CourseResourceVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseResourceController {

    private final CourseResourceService courseResourceService;

    @SaCheckPermission("course:view")
    @GetMapping("/{id}/resources")
    public ApiResult<List<CourseResourceVO>> listCourseResources(@PathVariable("id") Long courseId,
                                                                 @org.springframework.web.bind.annotation.RequestParam(value = "chapterId", required = false) Long chapterId) {
        return ApiResult.success(courseResourceService.listByCourseId(courseId, chapterId));
    }

    @SaCheckPermission("course:edit")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/resources")
    public ApiResult<Long> addCourseResource(@PathVariable("id") Long courseId,
                                             @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.edumind.resource.dto.course.CourseResourceCreateDTO dto) {
        return ApiResult.success(courseResourceService.addResource(courseId, dto));
    }

    @SaCheckPermission("course:edit")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/resources/{resourceId}")
    public ApiResult<Void> deleteCourseResource(@PathVariable("id") Long courseId,
                                                @PathVariable("resourceId") Long resourceId) {
        courseResourceService.deleteResource(resourceId);
        return ApiResult.success();
    }
}
