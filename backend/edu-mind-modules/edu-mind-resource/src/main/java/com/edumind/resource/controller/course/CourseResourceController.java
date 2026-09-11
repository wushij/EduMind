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
    public ApiResult<List<CourseResourceVO>> listCourseResources(@PathVariable("id") Long courseId) {
        return ApiResult.success(courseResourceService.listByCourseId(courseId));
    }
}
