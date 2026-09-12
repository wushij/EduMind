package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeMasteryVO {
    private List<String> dimensions = new ArrayList<>();
    private List<Integer> personal = new ArrayList<>();
    private List<Integer> classAvg = new ArrayList<>();
    private List<WeakPointVO> weakPoints = new ArrayList<>();

    @Data
    public static class WeakPointVO {
        private Long knowledgePointId;
        private String title;
        private Double mastery;
        private String suggestion;
    }
}
