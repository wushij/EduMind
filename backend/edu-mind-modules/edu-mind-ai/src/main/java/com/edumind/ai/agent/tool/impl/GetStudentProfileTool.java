package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.common.api.analytics.KnowledgeMasteryQueryApi;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetStudentProfileTool implements AgentTool {

    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;

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
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : null;
        Long studentId = params.get("studentId") != null
                ? Long.valueOf(params.get("studentId").toString())
                : LoginUserResolver.resolveUserId();
        if (courseId == null || studentId == null) {
            return Map.of("error", "courseId and studentId are required");
        }
        return knowledgeMasteryQueryApi.getStudentProfile(studentId, courseId);
    }
}
