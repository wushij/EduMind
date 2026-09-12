package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WrongQuestionAnalyticsVO {
    private List<WrongQuestionItemVO> list = new ArrayList<>();
    private Long total;

    @Data
    public static class WrongQuestionItemVO {
        private Long id;
        private Long questionId;
        private Integer wrongCount;
        private List<String> errorTypes;
        private String diagnosis;
        private List<Long> variantQuestionIds;
    }
}
