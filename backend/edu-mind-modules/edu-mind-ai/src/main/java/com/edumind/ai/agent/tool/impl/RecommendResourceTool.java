package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecommendResourceTool implements AgentTool {

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
        String kp = params.get("knowledgePoint") != null ? params.get("knowledgePoint").toString() : "多态";
        return Map.of(
                "knowledgePoint", kp,
                "resources", List.of(
                        Map.of("title", kp + "机制深入解析微课", "type", "VIDEO", "duration", "12分钟"),
                        Map.of("title", kp + "设计模式实战代码案例", "type", "CODE", "url", "https://edumind.example.com/demo/code")
                ),
                "summary", "已根据当前薄弱点智能匹配微课与实战项目案例"
        );
    }
}
