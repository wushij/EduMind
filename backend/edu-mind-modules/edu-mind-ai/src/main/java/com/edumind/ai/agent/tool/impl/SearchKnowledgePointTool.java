package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchKnowledgePointTool implements AgentTool {

    private final CourseQueryApi courseQueryApi;

    @Override
    public String name() {
        return "search_knowledge_point";
    }

    @Override
    public String description() {
        return "检索课程知识点";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long courseId = Long.valueOf(params.get("courseId").toString());
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        return points.stream().map(KnowledgePointVO::getTitle).toList();
    }
}
