package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningPathDetailVO {
    private Long courseId;
    private String courseName;
    private Long studentId;
    private String title;
    private Integer overallProgressPercent;
    private Integer weakPointCount;
    private Integer graphGapCount;
    private Integer estimatedTotalMinutes;
    private String generatedAt;
    private String interpretHint;
    private List<LearningPathVO.LearningPathWeekVO> weeks = new ArrayList<>();
    private GraphSliceVO graphSlice = new GraphSliceVO();
    private List<WeakPointBriefVO> weakPointsBrief = new ArrayList<>();

    @Data
    public static class WeakPointBriefVO {
        private Long knowledgePointId;
        private String title;
        private Double masteryPercent;
        private String suggestion;
    }

    @Data
    public static class GraphSliceVO {
        private List<GraphNodeVO> nodes = new ArrayList<>();
        private List<GraphEdgeVO> edges = new ArrayList<>();
        private List<String> highlightNodeIds = new ArrayList<>();
        private List<String> pathEdgeIds = new ArrayList<>();
    }

    @Data
    public static class GraphNodeVO {
        private String id;
        private String label;
        private String type;
        private Long refId;
        private Double masteryPercent;
        private String status;
    }

    @Data
    public static class GraphEdgeVO {
        private String id;
        private String source;
        private String target;
        private String relation;
    }
}
