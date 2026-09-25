package com.edumind.statistics.vo.teaching;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeachingReportVO {
    private Long courseId;
    private String courseName;
    private String courseCode;
    private String teacherName;
    private Integer studentCount;
    private Integer syllabusProgress;
    private String range;
    private Integer totalChapters;
    private Integer recommendedQuestions;
    private Integer recommendedResources;
    private Integer aiCallCount;
    private Double avgSubmissionRate;
    private Double knowledgeMasteryAvg;
    private List<WeakPointVO> weakPoints = new ArrayList<>();
    private List<WeeklyActivityVO> weeklyActivity = new ArrayList<>();
    private List<ErrorCategoryVO> errorCategories = new ArrayList<>();

    @Data
    public static class WeeklyActivityVO {
        private String date;
        private Integer count;
    }

    @Data
    public static class ErrorCategoryVO {
        private String type;
        private String name;
        private Integer percent;
    }

    @Data
    public static class WeakPointVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        private String questionStem;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgePointId;
        private String knowledgePointName;
        private String chapterName;
        private String title;
        private Integer wrongCount;
        private Integer masteryRate;
        private String errorType;
        private String errorTypeName;
        private String errorReason;
        private String suggestion;
        private String status;
        private String statusLabel;
    }
}

