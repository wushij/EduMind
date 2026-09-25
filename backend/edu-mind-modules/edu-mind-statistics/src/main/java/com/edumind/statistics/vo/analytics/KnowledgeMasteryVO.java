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

    private Integer totalKnowledgePoints = 0;
    private Double classAvgMastery = 0.0;
    private Integer masteredCount = 0;
    private Integer goodCount = 0;
    private Integer warningCount = 0;
    private Integer studentCount = 0;

    private List<StudentItemVO> students = new ArrayList<>();

    public List<String> getDimensions() { return dimensions; }
    public void setDimensions(List<String> dimensions) { this.dimensions = dimensions; }

    public List<Integer> getPersonal() { return personal; }
    public void setPersonal(List<Integer> personal) { this.personal = personal; }

    public List<Integer> getClassAvg() { return classAvg; }
    public void setClassAvg(List<Integer> classAvg) { this.classAvg = classAvg; }

    public List<WeakPointVO> getWeakPoints() { return weakPoints; }
    public void setWeakPoints(List<WeakPointVO> weakPoints) { this.weakPoints = weakPoints; }

    public Integer getTotalKnowledgePoints() { return totalKnowledgePoints; }
    public void setTotalKnowledgePoints(Integer totalKnowledgePoints) { this.totalKnowledgePoints = totalKnowledgePoints; }

    public Double getClassAvgMastery() { return classAvgMastery; }
    public void setClassAvgMastery(Double classAvgMastery) { this.classAvgMastery = classAvgMastery; }

    public Integer getMasteredCount() { return masteredCount; }
    public void setMasteredCount(Integer masteredCount) { this.masteredCount = masteredCount; }

    public Integer getGoodCount() { return goodCount; }
    public void setGoodCount(Integer goodCount) { this.goodCount = goodCount; }

    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public List<StudentItemVO> getStudents() { return students; }
    public void setStudents(List<StudentItemVO> students) { this.students = students; }

    @Data
    public static class StudentItemVO {
        private Long id;
        private String name;
        private String username;
        private String studentNo;
        private String avatar;
        private String className;
        private Double masteryAvg;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public Double getMasteryAvg() { return masteryAvg; }
        public void setMasteryAvg(Double masteryAvg) { this.masteryAvg = masteryAvg; }
    }

    @Data
    public static class WeakPointVO {
        private Long knowledgePointId;
        private String title;
        private String chapterName;
        private Double mastery;
        private String suggestion;
        private Integer wrongCount;
        private Integer affectedStudentCount;

        public Long getKnowledgePointId() { return knowledgePointId; }
        public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getChapterName() { return chapterName; }
        public void setChapterName(String chapterName) { this.chapterName = chapterName; }
        public Double getMastery() { return mastery; }
        public void setMastery(Double mastery) { this.mastery = mastery; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public Integer getWrongCount() { return wrongCount; }
        public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
        public Integer getAffectedStudentCount() { return affectedStudentCount; }
        public void setAffectedStudentCount(Integer affectedStudentCount) { this.affectedStudentCount = affectedStudentCount; }
    }
}


