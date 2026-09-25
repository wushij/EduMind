package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeMasteryVO {

    /** 统计作用域：全班口径（个人雷达线为空，薄弱榜按班级均分） */
    public static final String SCOPE_CLASS = "CLASS";
    /** 统计作用域：聚焦某位选课学员（个人雷达线与薄弱榜按该学员分值） */
    public static final String SCOPE_STUDENT = "STUDENT";

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

    /**
     * 当前统计作用域，取值 {@link #SCOPE_CLASS} 或 {@link #SCOPE_STUDENT}。
     * 前端据此决定指标条展示「班级均分」还是「学员均分」，
     * 以及「薄弱待攻坚 N 个」与薄弱考点榜单是否同源。
     */
    private String scope = SCOPE_CLASS;

    /** 聚焦学员 ID，未聚焦时为 null */
    private Long focusStudentId;

    /** 聚焦学员的掌握度均分，未聚焦时为 null */
    private Double focusAvgMastery;

    /** 课程真实选课成员总数（未过滤测试账号，便于展示"X / Y 人建档"） */
    private Integer classStudentCount = 0;

    /** 参与本次统计的方格中，来自真实测评的数量 */
    private Integer measuredCellCount = 0;

    /** 参与本次统计的方格总数（学员数 × 考点数） */
    private Integer totalCellCount = 0;

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

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Long getFocusStudentId() { return focusStudentId; }
    public void setFocusStudentId(Long focusStudentId) { this.focusStudentId = focusStudentId; }

    public Double getFocusAvgMastery() { return focusAvgMastery; }
    public void setFocusAvgMastery(Double focusAvgMastery) { this.focusAvgMastery = focusAvgMastery; }

    public Integer getClassStudentCount() { return classStudentCount; }
    public void setClassStudentCount(Integer classStudentCount) { this.classStudentCount = classStudentCount; }

    public Integer getMeasuredCellCount() { return measuredCellCount; }
    public void setMeasuredCellCount(Integer measuredCellCount) { this.measuredCellCount = measuredCellCount; }

    public Integer getTotalCellCount() { return totalCellCount; }
    public void setTotalCellCount(Integer totalCellCount) { this.totalCellCount = totalCellCount; }

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
        /** 该学员已有真实测评记录的考点数 */
        private Integer measuredKpCount = 0;
        /** 该学员依靠规则推算得出分值的考点数 */
        private Integer estimatedKpCount = 0;

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

        public Integer getMeasuredKpCount() { return measuredKpCount; }
        public void setMeasuredKpCount(Integer measuredKpCount) { this.measuredKpCount = measuredKpCount; }

        public Integer getEstimatedKpCount() { return estimatedKpCount; }
        public void setEstimatedKpCount(Integer estimatedKpCount) { this.estimatedKpCount = estimatedKpCount; }
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
        /** 该考点上真正有实测成绩的学员数 */
        private Integer measuredStudentCount = 0;
        /** 参与统计的班级学员数（受影响人数的分母） */
        private Integer classStudentCount = 0;
        /**
         * 数据可信度：MEASURED / MIXED / ESTIMATED。
         * 全部学员均为推算时给出 ESTIMATED，提示教师该结论尚未被真实测评验证。
         */
        private String dataConfidence = "ESTIMATED";

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

        public Integer getMeasuredStudentCount() { return measuredStudentCount; }
        public void setMeasuredStudentCount(Integer measuredStudentCount) { this.measuredStudentCount = measuredStudentCount; }

        public Integer getClassStudentCount() { return classStudentCount; }
        public void setClassStudentCount(Integer classStudentCount) { this.classStudentCount = classStudentCount; }

        public String getDataConfidence() { return dataConfidence; }
        public void setDataConfidence(String dataConfidence) { this.dataConfidence = dataConfidence; }
    }
}


