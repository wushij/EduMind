package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningAnalyticsVO {
    private Long courseId;
    private Integer studentCount;
    private Double completionRate;
    private Double avgScore;
    private Integer avgStudyMinutes;
    private Double knowledgeMasteryAvg;
    private Integer aiUsageCount;
    private Boolean aggregated;
    /** 数据更新时间：统计范围内最近一次真实学习行为时间（yyyy-MM-dd HH:mm），无数据时为 null */
    private String dataUpdatedAt;
    private List<StudentLearningItemVO> students = new ArrayList<>();
    private TrendData trends = new TrendData();

    // 课程深入分析维度
    private List<ChapterProgressVO> chapterProgressList = new ArrayList<>();
    private CourseHealthVO courseHealth;
    private List<CourseWeakPointVO> courseWeakPoints = new ArrayList<>();

    @Data
    public static class TrendData {
        private List<TrendPoint> learning = new ArrayList<>();
        private List<ScoreTrendPoint> score = new ArrayList<>();
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer activeUsers;
    }

    @Data
    public static class ScoreTrendPoint {
        private String date;
        private Double avgScore;
        private Double schoolAvgScore; // 全校/年级对照平均分
    }

    @Data
    public static class ChapterProgressVO {
        private Long chapterId;
        private String chapterTitle;
        private Integer sort;
        /** 章节学习覆盖率 (0~100)：本章（含其微课节）有学习行为的学生数 / 选课学生数 */
        private Double completionRate;
        /** 章节掌握度 (0~100)：本章关联知识点的平均掌握度；课程未挂知识点时为 null */
        private Double avgScore;
        private Integer studentCount; // 本章去重学习人数
        private Integer avgStudyMinutes; // 本章人均学习时长（分钟）
    }

    @Data
    public static class CourseHealthVO {
        private Double overallScore; // 综合健康度得分 (0~100)
        private Integer syllabusCoverage; // 考点覆盖度 (0~100)
        private Integer assignmentCompletion; // 作业完成度 (0~100)
        private Integer studentInteraction; // 师生互动度 (0~100)
        private Integer passRate; // 测验及格率 (0~100)
        private Integer aiAssistanceRate; // AI 辅导渗透率 (0~100)
        private String healthLevel; // EXCELLENT, GOOD, WARNING
    }

    @Data
    public static class CourseWeakPointVO {
        private Long knowledgePointId;
        private String title;
        private Double mastery; // 掌握度百分比 (0~100)
        private Integer wrongCount; // 错题次数
        private Integer affectedStudents; // 需关注学生数
        private String urgency; // HIGH, MEDIUM, LOW
    }
}
