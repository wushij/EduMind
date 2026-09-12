package com.edumind.statistics.vo.teaching;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeachingReportVO {
    private Long courseId;
    private String range;
    private Integer totalChapters;
    private Integer recommendedQuestions;
    private Integer recommendedResources;
    private Integer aiCallCount;
    private Double avgSubmissionRate;
    private List<WeakPointVO> weakPoints = new ArrayList<>();

    @Data
    public static class WeakPointVO {
        private String title;
        private Integer wrongCount;
        private String suggestion;
    }
}
