package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.service.grading.AiGradingService;
import com.edumind.ai.vo.SubjectiveGradingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GradeAnswerTool implements AgentTool {

    private final AiGradingService aiGradingService;

    @Override
    public String name() {
        return "grade_answer";
    }

    @Override
    public String description() {
        return "智能批改学生主观题或编程题作答并给出评分与归因";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String question = params.get("question") != null ? params.get("question").toString() : "简述面向对象三大特征";
        String standardAnswer = params.get("standardAnswer") != null ? params.get("standardAnswer").toString() : "封装、继承、多态";
        String studentAnswer = params.get("studentAnswer") != null ? params.get("studentAnswer").toString() : "封装和多态";
        int totalScore = params.get("totalScore") != null ? Integer.parseInt(params.get("totalScore").toString()) : 10;

        SubjectiveGradingDTO dto = new SubjectiveGradingDTO();
        dto.setQuestionStem(question);
        dto.setReferenceAnswer(standardAnswer);
        dto.setStudentAnswer(studentAnswer);
        dto.setMaxScore(totalScore);

        SubjectiveGradingVO vo = aiGradingService.gradeSubjective(dto);
        return Map.of(
                "score", vo.getScore() != null ? vo.getScore() : 8,
                "feedback", vo.getAiComment() != null ? vo.getAiComment() : "基本正确，略有遗漏",
                "isCorrect", vo.getScore() != null && vo.getScore() >= totalScore * 0.6
        );
    }
}
