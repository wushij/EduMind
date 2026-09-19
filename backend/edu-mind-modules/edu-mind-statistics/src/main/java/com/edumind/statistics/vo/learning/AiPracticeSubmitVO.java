package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class AiPracticeSubmitVO {
    private String sessionId;
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracyRate;
    private Integer durationSeconds;
    private java.util.List<Long> wrongQuestionIds = new java.util.ArrayList<>();
    private java.util.List<Long> weakKnowledgePointIds = new java.util.ArrayList<>();
    private String aiSummary;
    private java.util.List<QuestionResultItem> questionResults = new java.util.ArrayList<>();

    @Data
    public static class QuestionResultItem {
        private Long questionId;
        private Boolean correct;
        private String studentAnswer;
        private String referenceAnswer;
        private String analysis;
        private String knowledgePointName;
    }
}
