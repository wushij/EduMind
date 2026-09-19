package com.edumind.statistics.vo.analytics;

import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生个体学情画像模型
 */
@Data
public class StudentPortraitVO implements Serializable {

    private StudentInfoVO studentInfo = new StudentInfoVO();
    private PortraitSummaryVO summary = new PortraitSummaryVO();
    private RadarDataVO radar = new RadarDataVO();
    private List<KnowledgePointMasteryItemVO> knowledgePoints = new ArrayList<>();
    private List<WeakPointVO> weakPoints = new ArrayList<>();
    private List<MasteredPointVO> masteredPoints = new ArrayList<>();
    private List<StudentWrongItemVO> wrongQuestions = new ArrayList<>();
    private List<LearningPathVO.LearningPathWeekVO> adaptiveWeeks = new ArrayList<>();
    private String aiDiagnosis;

    @Data
    public static class StudentInfoVO implements Serializable {
        private Long studentId;
        private String username;
        private String realName;
        private String avatar;
        private String studentNo;
        private String className;
        private String role;
        private String lastActiveTime;
    }

    @Data
    public static class PortraitSummaryVO implements Serializable {
        private Integer totalStudyMinutes = 0;
        /** 累计学时（不受 range 筛选影响） */
        private Integer totalStudyMinutesAllTime = 0;
        private Double classAvgStudyMinutes = 0.0;
        private Double avgScore = 0.0;
        private Double classAvgScore = 0.0;
        private Double submissionRate = 0.0;
        private Double overallMastery = 0.0;
        private Integer aiUsageCount = 0;
        private Integer wrongQuestionCount = 0;
        private String learningPace; // 'FAST', 'NORMAL', 'STEADY', 'LAGGING'
    }

    @Data
    public static class RadarDataVO implements Serializable {
        private List<String> dimensions = new ArrayList<>();
        private List<Integer> personalScores = new ArrayList<>();
        private List<Integer> classAvgScores = new ArrayList<>();
    }

    @Data
    public static class KnowledgePointMasteryItemVO implements Serializable {
        private Long knowledgePointId;
        private String title;
        private Double masteryScore;
        private Integer sampleCount;
        private String status; // 'MASTERED', 'LEARNING', 'WEAK'
        private String lastAssessedAt;
        private String suggestion;
    }

    @Data
    public static class WeakPointVO implements Serializable {
        private Long knowledgePointId;
        private String title;
        private Double mastery;
        private String suggestion;
    }

    @Data
    public static class MasteredPointVO implements Serializable {
        private Long knowledgePointId;
        private String title;
        private Double mastery;
    }

    @Data
    public static class StudentWrongItemVO implements Serializable {
        private Long recordId;
        private Long questionId;
        private String questionStem;
        private Long knowledgePointId;
        private String knowledgePointTitle;
        private String errorTypes;
        private String diagnosis;
        private Integer wrongCount;
        private String createTime;
    }
}
