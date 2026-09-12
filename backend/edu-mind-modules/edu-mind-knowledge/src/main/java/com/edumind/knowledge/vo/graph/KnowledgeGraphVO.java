package com.edumind.knowledge.vo.graph;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeGraphVO {
    private List<GraphNodeVO> nodes = new ArrayList<>();
    private List<GraphEdgeVO> edges = new ArrayList<>();

    @Data
    public static class GraphNodeVO {
        private String id;
        private String label;
        private String type;
        private Long refId;
    }

    @Data
    public static class GraphEdgeVO {
        private String source;
        private String target;
        private String relation;
    }
}
