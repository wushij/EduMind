package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.statistics.api.RecommendationQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecommendResourceTool implements AgentTool {

    private final RecommendationQueryApi recommendationQueryApi;

    @Override
    public String name() {
        return "recommend_resource";
    }

    @Override
    public String description() {
        return "为指定知识点或薄弱环节推荐课外拓展资料与微课视频";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : null;
        Long chapterId = params.get("chapterId") != null ? Long.valueOf(params.get("chapterId").toString()) : null;
        Integer limit = params.get("limit") != null ? Integer.valueOf(params.get("limit").toString()) : 5;
        String kp = params.get("knowledgePoint") != null ? params.get("knowledgePoint").toString() : "";

        List<Map<String, Object>> resources = recommendationQueryApi.recommendResources(courseId, chapterId, limit);
        return Map.of(
                "knowledgePoint", kp,
                "resources", resources,
                "summary", resources.isEmpty()
                        ? "暂未找到匹配资源，请补充课程资源库"
                        : "已根据课程资源库匹配 " + resources.size() + " 条推荐资源"
        );
    }
}
