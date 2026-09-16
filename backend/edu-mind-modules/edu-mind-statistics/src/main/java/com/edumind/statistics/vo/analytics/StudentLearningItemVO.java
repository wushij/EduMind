package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程班级选课学生学情概览项
 */
@Data
public class StudentLearningItemVO implements Serializable {

    private Long studentId;
    private String username;
    private String realName;
    private String studentNo;
    private String avatar;
    private Integer studyMinutes;
    private Double avgScore;
    private Double submissionRate;
    private Double masteryScore;
    private Integer aiUsageCount;
    private Integer wrongCount;
    /**
     * 学情健康状态: EXCELLENT(优秀), GOOD(良好), WARNING(需关注), RISK(预警)
     */
    private String status;
}
