package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchCourseTool implements AgentTool {

    private final CourseQueryApi courseQueryApi;

    @Override
    public String name() {
        return "search_course";
    }

    @Override
    public String description() {
        return "查询课程详情及教学目标信息";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : 102L;
        CourseDetailVO detail = courseQueryApi.getCourseById(courseId);
        if (detail == null) {
            return Map.of("error", "未找到指定课程信息", "courseId", courseId);
        }
        return Map.of(
                "courseId", detail.getId(),
                "title", detail.getName() != null ? detail.getName() : "未知课程",
                "code", detail.getCode() != null ? detail.getCode() : "",
                "description", detail.getDescription() != null ? detail.getDescription() : ""
        );
    }
}
