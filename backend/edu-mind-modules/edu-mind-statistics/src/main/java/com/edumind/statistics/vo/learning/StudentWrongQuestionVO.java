package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentWrongQuestionVO {
    private Long total;
    private List<Item> list = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;
        private Long questionId;
        private Long knowledgePointId;
        private Integer wrongCount;
        private String diagnosis;
        private List<String> errorTypes = new ArrayList<>();
    }
}
