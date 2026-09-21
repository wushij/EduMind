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
        /** 截断后的题干预览（保留兼容；公式可能被截断，展示请优先用 stem） */
        private String stemPreview;
        /** 完整题干（裸 LaTeX 已补全 $ 定界符），由前端按行数裁切展示，保证公式完整渲染 */
        private String stem;
    }
}
