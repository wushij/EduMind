package com.edumind.knowledge.vo.graph;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GraphGapVO {
    private Long knowledgePointId;
    private String title;
    private List<PrerequisiteVO> missingPrerequisites = new ArrayList<>();

    @Data
    public static class PrerequisiteVO {
        private Long id;
        private String title;
    }
}
