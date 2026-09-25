package com.edumind.statistics.vo.learning;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class RecommendedQuestionVO {
    private Long id;
    private String stem;
    private String type;
    private Integer difficulty;
    private Long courseId;
    private String courseName;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Integer matchScore;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }

    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
