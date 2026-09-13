package com.edumind.statistics.dto.learning;

import lombok.Data;

import java.util.List;

@Data
public class AiPracticeSubmitDTO {
    private Long courseId;
    private String sessionId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private Long knowledgePointId;
        private String answer;
        private Boolean correct;
    }
}
