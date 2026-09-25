package com.edumind.statistics.vo.learning;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class LearningPathVO {
    private Long courseId;
    private String title;
    private List<LearningPathWeekVO> weeks = new ArrayList<>();

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<LearningPathWeekVO> getWeeks() { return weeks; }
    public void setWeeks(List<LearningPathWeekVO> weeks) { this.weeks = weeks; }

    public static class LearningPathWeekVO {
        private Integer weekNo;
        private String theme;
        private Long knowledgePointId;
        private Double masteryPercent;
        private String focusReason;
        private List<LearningPathTaskVO> tasks = new ArrayList<>();

        public Integer getWeekNo() { return weekNo; }
        public void setWeekNo(Integer weekNo) { this.weekNo = weekNo; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public Long getKnowledgePointId() { return knowledgePointId; }
        public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }

        public Double getMasteryPercent() { return masteryPercent; }
        public void setMasteryPercent(Double masteryPercent) { this.masteryPercent = masteryPercent; }

        public String getFocusReason() { return focusReason; }
        public void setFocusReason(String focusReason) { this.focusReason = focusReason; }

        public List<LearningPathTaskVO> getTasks() { return tasks; }
        public void setTasks(List<LearningPathTaskVO> tasks) { this.tasks = tasks; }
    }

    public static class LearningPathTaskVO {
        private String id;
        private String title;
        private String type;
        private String typeLabel;
        private Long refId;
        private Long knowledgePointId;
        private String status;
        private Integer estimatedMinutes;
        private String targetUrl;
        private String actionLabel;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }

        public Long getRefId() { return refId; }
        public void setRefId(Long refId) { this.refId = refId; }

        public Long getKnowledgePointId() { return knowledgePointId; }
        public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getEstimatedMinutes() { return estimatedMinutes; }
        public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

        public String getActionLabel() { return actionLabel; }
        public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    }
}
