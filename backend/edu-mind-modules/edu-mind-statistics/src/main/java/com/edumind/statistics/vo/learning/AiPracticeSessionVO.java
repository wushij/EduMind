package com.edumind.statistics.vo.learning;

import com.edumind.question.vo.question.QuestionVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiPracticeSessionVO {
    private String sessionId;
    private Long courseId;
    private Integer questionCount;
    private List<QuestionVO> questions = new ArrayList<>();
    private String weakPointHint;
    private Integer estimatedMinutes;
    private Integer weakKnowledgePointCount;
    private Long pendingWrongQuestionCount;
}
