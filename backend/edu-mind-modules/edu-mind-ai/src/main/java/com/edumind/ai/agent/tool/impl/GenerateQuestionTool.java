package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.service.question.QuestionGenerateService;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GenerateQuestionTool implements AgentTool {

    private final QuestionGenerateService questionGenerateService;

    @Override
    public String name() {
        return "generate_question";
    }

    @Override
    public String description() {
        return "根据课程与知识点生成题目";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        QuestionGenerateDTO dto = new QuestionGenerateDTO();
        dto.setCourseId(Long.valueOf(params.get("courseId").toString()));
        dto.setCount(params.get("count") != null ? Integer.parseInt(params.get("count").toString()) : 5);
        dto.setDifficulty(params.get("difficulty") != null ? params.get("difficulty").toString() : "MEDIUM");
        dto.setQuestionTypes(List.of("SINGLE_CHOICE"));
        List<QuestionVO> questions = questionGenerateService.generate(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("count", questions.size());
        result.put("questionIds", questions.stream().map(QuestionVO::getId).collect(Collectors.toList()));
        return result;
    }
}
