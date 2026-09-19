package com.edumind.statistics.dto.learning;

import lombok.Data;

import java.util.List;

@Data
public class AiPracticeSubmitDTO {
    private Long courseId;
    private String sessionId;
    /** 客户端统计用时（秒），服务端会做合理上限校验 */
    private Integer durationSeconds;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private Long knowledgePointId;
        private String answer;
        private Boolean correct;
    }
}
