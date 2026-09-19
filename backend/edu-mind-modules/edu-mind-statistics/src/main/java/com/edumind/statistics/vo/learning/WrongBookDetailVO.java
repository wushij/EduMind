package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WrongBookDetailVO extends WrongBookItemVO {

    private List<KnowledgeGraphNodeVO> prerequisiteNodes = new ArrayList<>();
    private List<VariantQuestionSummaryVO> variantQuestions = new ArrayList<>();

    @Data
    public static class KnowledgeGraphNodeVO {
        private Long knowledgePointId;
        private String name;
        /** 掌握度 0~100 */
        private Integer masteryPercent;
        private boolean current;
    }

    @Data
    public static class VariantQuestionSummaryVO {
        private Long questionId;
        private String stemPreview;
    }
}
