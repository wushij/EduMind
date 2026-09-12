package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetStudentProfileTool implements AgentTool {

    @Override
    public String name() {
        return "get_student_profile";
    }

    @Override
    public String description() {
        return "获取学生或班级的学情画像与薄弱知识点清单";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : 102L;
        Long studentId = params.get("studentId") != null ? Long.valueOf(params.get("studentId").toString()) : 3L;

        return Map.of(
                "studentId", studentId,
                "courseId", courseId,
                "overallMastery", 0.74,
                "weakKnowledgePoints", List.of("多态与动态绑定", "抽象类与接口"),
                "masteredKnowledgePoints", List.of("基本数据类型", "类与对象封装"),
                "recommendedReviewDurationMinutes", 45
        );
    }
}
