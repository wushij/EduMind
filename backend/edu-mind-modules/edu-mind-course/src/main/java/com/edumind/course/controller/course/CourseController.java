package com.edumind.course.controller.course;

import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.course.dto.course.CourseCreateDTO;
import com.edumind.course.dto.course.CourseQueryDTO;
import com.edumind.course.service.chapter.ChapterService;
import com.edumind.course.service.course.CourseService;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.edumind.course.dto.course.CourseUpdateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final ChapterService chapterService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<PageResult<CourseVO>> listCourses(CourseQueryDTO query) {
        return ApiResult.success(courseService.listCourses(query));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{id}")
    public ApiResult<CourseDetailVO> getCourseById(@PathVariable("id") Long id) {
        return ApiResult.success(courseService.getCourseById(id));
    }

    @SaCheckPermission("course:create")
    @PostMapping
    public ApiResult<Long> createCourse(@Valid @RequestBody CourseCreateDTO dto) {
        return ApiResult.success(courseService.createCourse(dto));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> updateCourse(@PathVariable("id") Long id,
                                        @Valid @RequestBody CourseUpdateDTO dto) {
        courseService.updateCourse(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("course:edit")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable("id") Long id) {
        courseService.deleteCourse(id);
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{id}/chapters")
    public ApiResult<List<ChapterTreeVO>> getChapterTree(@PathVariable("id") Long id) {
        return ApiResult.success(chapterService.getChapterTree(id));
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/{id}/chapters")
    public ApiResult<Long> createChapter(@PathVariable("id") Long id,
                                         @RequestBody java.util.Map<String, Object> body) {
        String title = (String) body.get("title");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : 0L;
        Integer sortOrder = body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : 1;
        com.edumind.course.dto.chapter.ChapterCreateDTO dto = new com.edumind.course.dto.chapter.ChapterCreateDTO();
        dto.setTitle(title);
        dto.setParentId(parentId);
        dto.setSortOrder(sortOrder);
        if (body.get("description") != null) {
            dto.setDescription(body.get("description").toString());
        }
        if (body.get("durationMinutes") != null) {
            dto.setDurationMinutes(Integer.valueOf(body.get("durationMinutes").toString()));
        } else if (body.get("duration") != null) {
            String duration = body.get("duration").toString().replace("分钟", "").trim();
            if (!duration.isEmpty()) {
                dto.setDurationMinutes(Integer.parseInt(duration));
            }
        }
        if (body.get("lessonType") != null) {
            dto.setLessonType(body.get("lessonType").toString());
        } else if (body.get("type") != null) {
            dto.setLessonType(body.get("type").toString());
        }
        return ApiResult.success(chapterService.createChapter(id, dto));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/{id}/chapters/{chapterId}")
    public ApiResult<Void> updateChapter(@PathVariable("id") Long id,
                                         @PathVariable("chapterId") Long chapterId,
                                         @RequestBody java.util.Map<String, Object> body) {
        String title = (String) body.get("title");
        Integer sortOrder = body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : null;
        chapterService.updateChapter(id, chapterId, title, sortOrder);
        return ApiResult.success();
    }

    @SaCheckPermission("course:edit")
    @DeleteMapping("/{id}/chapters/{chapterId}")
    public ApiResult<Void> deleteChapter(@PathVariable("id") Long id,
                                         @PathVariable("chapterId") Long chapterId) {
        chapterService.deleteChapter(id, chapterId);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{id}/knowledge-points")
    public ApiResult<List<KnowledgePointVO>> listKnowledgePoints(@PathVariable("id") Long id,
                                                                 @RequestParam(value = "chapterId", required = false) Long chapterId) {
        return ApiResult.success(courseService.listKnowledgePoints(id, chapterId));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/{id}/knowledge-points/{kpId}")
    public ApiResult<KnowledgePointVO> getKnowledgePoint(@PathVariable("id") Long id,
                                                         @PathVariable("kpId") Long kpId) {
        return ApiResult.success(courseService.getKnowledgePoint(id, kpId));
    }

    @SaCheckPermission("course:edit")
    @PostMapping("/{id}/knowledge-points")
    public ApiResult<KnowledgePointVO> createKnowledgePoint(@PathVariable("id") Long id,
                                                            @Valid @RequestBody com.edumind.course.dto.knowledge.KnowledgePointCreateDTO dto) {
        return ApiResult.success(courseService.createKnowledgePoint(id, dto));
    }

    @SaCheckPermission("course:edit")
    @PutMapping("/{id}/knowledge-points/{kpId}")
    public ApiResult<KnowledgePointVO> updateKnowledgePoint(@PathVariable("id") Long id,
                                                             @PathVariable("kpId") Long kpId,
                                                             @Valid @RequestBody com.edumind.course.dto.knowledge.KnowledgePointUpdateDTO dto) {
        return ApiResult.success(courseService.updateKnowledgePoint(id, kpId, dto));
    }

    @SaCheckPermission("course:edit")
    @DeleteMapping("/{id}/knowledge-points/{kpId}")
    public ApiResult<Void> deleteKnowledgePoint(@PathVariable("id") Long id,
                                                @PathVariable("kpId") Long kpId) {
        courseService.deleteKnowledgePoint(id, kpId);
        return ApiResult.success();
    }

    @SaCheckPermission("course:view")
    @PostMapping("/join")
    public ApiResult<Long> joinCourse(@Valid @RequestBody com.edumind.course.dto.course.CourseJoinDTO dto) {
        return ApiResult.success(courseService.joinCourseByCode(dto.getCode()));
    }

    @SaCheckPermission("course:view")
    @GetMapping("/public")
    public ApiResult<List<CourseVO>> listPublicCourses() {
        return ApiResult.success(courseService.listPublicCourses());
    }
}
