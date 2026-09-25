package com.edumind.statistics.vo.intervention;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 教学干预建议展示视图对象 VO (支持真实学情联动、靶向微课与试题画像)
 */
@Data
public class TeachingInterventionVO implements Serializable {

    private Long id;

    private Long tenantId;

    private Long courseId;

    private String courseName;

    /**
     * 关联薄弱知识点 ID
     */
    private Long knowledgePointId;

    /**
     * 关联薄弱知识点名称
     */
    private String knowledgePointTitle;

    /**
     * 触发动因 (EXAM_WEAK: 考试薄弱断层 / ACTIVITY_DROP: 活跃度骤降 / HOMEWORK_DELAY: 作业滞后)
     */
    private String triggerType;

    private String title;

    private String proposalText;

    private Integer affectedStudentCount;

    private String status; // PENDING / APPROVED / DISPATCHED / REVOKED

    private String approvedBy;

    private LocalDateTime createTime;

    private LocalDateTime dispatchedTime;

    /**
     * 预期掌握度提升（如："+15% ~ +22%"）
     */
    private String expectedImprovement;

    /**
     * 真实关联的目标预警学生群组
     */
    private List<TargetStudentVO> targetStudents = new ArrayList<>();

    /**
     * 绑定的靶向微课与课件资源清单
     */
    private List<InterventionResourceVO> resources = new ArrayList<>();

    /**
     * 绑定的靶向强化变式题清单
     */
    private List<InterventionQuestionVO> questions = new ArrayList<>();

    /**
     * 闭环跟踪统计
     */
    private InterventionTrackingStatsVO trackingStats;

    @Data
    public static class TargetStudentVO implements Serializable {
        private Long studentId;
        private String realName;
        private String username;
        private String studentNo;
        private String avatar;
        private Double score;
        private String riskLevel; // RISK, WARNING
    }

    @Data
    public static class InterventionResourceVO implements Serializable {
        private Long resourceId;
        private String title;
        private String type; // VIDEO, DOCUMENT
        private String duration; // 如 "8分20秒"
        private String url;
        private String description;
    }

    @Data
    public static class InterventionQuestionVO implements Serializable {
        private Long questionId;
        private String stem;
        private String type;
        private String difficulty;
        private List<String> options = new ArrayList<>();
        private String answer;
        private String analysis;
    }

    @Data
    public static class InterventionTrackingStatsVO implements Serializable {
        private Integer dispatchedCount;
        private Integer completedCount;
        private Double avgImprovementScore;
    }
}
