package com.edumind.statistics.service.learning.support;

import com.edumind.ai.api.AiGradingApi;
import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.vo.SubjectiveGradingVO;
import com.edumind.common.enums.QuestionType;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.vo.learning.AiPracticeGradeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiPracticeAnswerGrader {

    private static final int SUBJECTIVE_PASS_RATIO = 60;

    private final AiGradingApi aiGradingApi;

    public AiPracticeGradeVO grade(QuestionVO question, String studentAnswer) {
        AiPracticeGradeVO vo = new AiPracticeGradeVO();
        vo.setQuestionId(question.getId());
        vo.setReferenceAnswer(question.getAnswer());
        vo.setKnowledgePointName(question.getKnowledgePointName());
        vo.setAnalysis(StringUtils.hasText(question.getAnalysis())
                ? question.getAnalysis()
                : "请结合课程讲义完成本题。");

        int maxScore = question.getScore() != null && question.getScore() > 0 ? question.getScore() : 5;
        vo.setMaxScore(maxScore);

        if (!StringUtils.hasText(studentAnswer)) {
            vo.setCorrect(false);
            vo.setScore(0);
            return vo;
        }

        if (isObjectiveType(question.getType())) {
            boolean correct = normalizeObjectiveAnswer(studentAnswer)
                    .equals(normalizeObjectiveAnswer(question.getAnswer()));
            vo.setCorrect(correct);
            vo.setScore(correct ? maxScore : 0);
            if (!correct && StringUtils.hasText(question.getAnalysis())) {
                vo.setAnalysis(question.getAnalysis());
            }
            return vo;
        }

        SubjectiveGradingDTO dto = new SubjectiveGradingDTO();
        dto.setQuestionId(question.getId());
        dto.setCourseId(question.getCourseId());
        dto.setQuestionStem(question.getStem());
        dto.setReferenceAnswer(question.getAnswer());
        dto.setStudentAnswer(studentAnswer);
        dto.setMaxScore(maxScore);
        SubjectiveGradingVO aiResult = aiGradingApi.gradeSubjective(dto);
        int score = aiResult.getScore() != null ? aiResult.getScore() : 0;
        vo.setScore(score);
        vo.setCorrect(score * 100 >= maxScore * SUBJECTIVE_PASS_RATIO);
        if (StringUtils.hasText(aiResult.getAiComment())) {
            vo.setAnalysis(aiResult.getAiComment());
        }
        return vo;
    }

    public static boolean isObjectiveType(String type) {
        if (!StringUtils.hasText(type)) {
            return true;
        }
        String t = type.trim().toUpperCase(Locale.ROOT);
        return QuestionType.SINGLE_CHOICE.getCode().equals(t)
                || QuestionType.MULTIPLE_CHOICE.getCode().equals(t)
                || QuestionType.TRUE_FALSE.getCode().equals(t)
                || QuestionType.FILL_BLANK.getCode().equals(t)
                || "JUDGE".equals(t)
                || "SINGLE".equals(t);
    }

    public static String normalizeObjectiveAnswer(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        if (trimmed.contains(",")) {
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        return trimmed;
    }
}
