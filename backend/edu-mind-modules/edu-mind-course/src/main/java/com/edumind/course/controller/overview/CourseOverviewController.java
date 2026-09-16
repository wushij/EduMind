package com.edumind.course.controller.overview;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.course.dto.overview.CourseAnnouncementCreateDTO;
import com.edumind.course.dto.overview.CourseAnnouncementUpdateDTO;
import com.edumind.course.dto.overview.CourseInstructorsSaveDTO;
import com.edumind.course.dto.overview.CourseObjectivesSaveDTO;
import com.edumind.course.service.overview.CourseAnnouncementService;
import com.edumind.course.service.overview.CourseInstructorProfileService;
import com.edumind.course.service.overview.CourseObjectiveService;
import com.edumind.course.service.overview.CourseOverviewService;
import com.edumind.course.vo.overview.CourseAnnouncementVO;
import com.edumind.course.vo.overview.CourseInstructorCardVO;
import com.edumind.course.vo.overview.CourseObjectiveVO;
import com.edumind.course.vo.overview.CourseOverviewVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}")
@RequiredArgsConstructor
public class CourseOverviewController {

    private final CourseOverviewService overviewService;
    private final CourseObjectiveService objectiveService;
    private final CourseAnnouncementService announcementService;
    private final CourseInstructorProfileService instructorProfileService;

    @SaCheckPermission("course:view")
    @GetMapping("/overview")
    public ApiResult<CourseOverviewVO> getOverview(@PathVariable("courseId") Long courseId) {
        return ApiResult.success(overviewService.getOverview(courseId));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/objectives")
    public ApiResult<List<CourseObjectiveVO>> listObjectives(@PathVariable("courseId") Long courseId) {
        return ApiResult.success(objectiveService.list(courseId));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/objectives")
    public ApiResult<Void> saveObjectives(@PathVariable("courseId") Long courseId,
                                          @Valid @RequestBody CourseObjectivesSaveDTO dto) {
        objectiveService.saveAll(courseId, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @GetMapping("/announcements")
    public ApiResult<PageResult<CourseAnnouncementVO>> pageAnnouncements(
            @PathVariable("courseId") Long courseId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResult.success(announcementService.page(courseId, page, pageSize, status));
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/announcements")
    public ApiResult<CourseAnnouncementVO> createAnnouncement(
            @PathVariable("courseId") Long courseId,
            @Valid @RequestBody CourseAnnouncementCreateDTO dto) {
        return ApiResult.success(announcementService.create(courseId, dto));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/announcements/{announcementId}")
    public ApiResult<CourseAnnouncementVO> updateAnnouncement(
            @PathVariable("courseId") Long courseId,
            @PathVariable("announcementId") Long announcementId,
            @Valid @RequestBody CourseAnnouncementUpdateDTO dto) {
        return ApiResult.success(announcementService.update(courseId, announcementId, dto));
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/announcements/{announcementId}/withdraw")
    public ApiResult<Void> withdrawAnnouncement(
            @PathVariable("courseId") Long courseId,
            @PathVariable("announcementId") Long announcementId) {
        announcementService.withdraw(courseId, announcementId);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @GetMapping("/instructors")
    public ApiResult<List<CourseInstructorCardVO>> listInstructors(@PathVariable("courseId") Long courseId) {
        return ApiResult.success(instructorProfileService.listCards(courseId));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/instructors")
    public ApiResult<Void> saveInstructors(@PathVariable("courseId") Long courseId,
                                           @Valid @RequestBody CourseInstructorsSaveDTO dto) {
        instructorProfileService.saveAll(courseId, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/instructors/sync")
    public ApiResult<Void> syncInstructors(@PathVariable("courseId") Long courseId) {
        instructorProfileService.syncFromMembers(courseId);
        return ApiResult.success();
    }
}
