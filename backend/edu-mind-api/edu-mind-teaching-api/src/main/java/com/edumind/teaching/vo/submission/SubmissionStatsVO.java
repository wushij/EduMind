package com.edumind.teaching.vo.submission;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubmissionStatsVO {
    private Long courseId;
    private Integer totalAssignments;
    private Integer submittedCount;
    private Integer gradedCount;
    /**
     * 应提交答卷槽位数 = Σ(每份已发布/已关闭作业的选课学生数)。
     * 由本 VO 直接给出，调用方无需为了还原分母再查一次选课名单。
     */
    private Integer expectedSubmissionCount;
    private Double avgSubmissionRate;
    private Double avgScore;
    /**
     * 学生维度成绩明细（均分 / 答卷数）。
     * 与课程级汇总在同一次答卷扫描中产出，调用方不要再单独发起全量扫描。
     */
    private List<StudentScoreVO> studentScores = new ArrayList<>();

    @Data
    public static class StudentScoreVO {
        private Long studentId;
        private Double avgScore;
        private Integer submissionCount;
    }

    /** 按提交日期聚合的真实均分（百分制归一），用于成绩演进趋势 */
    @Data
    public static class DailyScoreVO {
        /** 统计日期 yyyy-MM-dd */
        private String date;
        /** 当日已批改答卷的归一平均分（0~100） */
        private Double avgScore;
        /** 当日纳入统计的答卷份数，用于前端判断样本是否充足 */
        private Integer sampleCount;
    }
}
