package com.edumind.knowledge.controller.lesson;

import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.api.LessonContentIndexApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class LessonCopilotIndexAdminController {

    private final LessonContentIndexApi lessonContentIndexApi;

    @PostMapping("/reindex-published")
    public ApiResult<Map<String, Object>> reindexPublished(@RequestParam(required = false) Long courseId) {
        int count = lessonContentIndexApi.reindexPublishedLessons(courseId);
        return ApiResult.success(Map.of("reindexed", count, "courseId", courseId));
    }
}
