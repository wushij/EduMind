package com.edumind.statistics.vo.teaching;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 教学报告聚合视图。
 *
 * <p>口径声明：本 VO 的每个指标都必须能追溯到真实数据源，
 * 无数据时返回 {@code null} 或 {@code 0}，由前端展示"暂无数据"，
 * 严禁在后端合成"看起来合理"的数值（例如按章节数推算大纲进度、按公式推算掌握度）。</p>
 */
@Data
public class TeachingReportVO {
    private Long courseId;
    private String courseName;
    private String courseCode;
    private String teacherName;

    /** 选课学生数（课程选课表），无数据为 0 */
    private Integer studentCount;

    /**
     * 教学大纲推进度 (0~100)：统计周期内产生过学习行为的章节数 / 课程顶层章节数。
     * 数据源：learning_record 按章节去重上卷；周期内无任何学习行为时为 0。
     */
    private Integer syllabusProgress;

    private String range;
    private Integer totalChapters;

    /** AI 答疑调用次数：优先取 course_statistics 预聚合，兜底 ai_call_log / 知识库维度 */
    private Integer aiCallCount;

    /**
     * 作业提交率 (0~100)：已提交答卷数 / 应提交答卷槽位数。
     * 应提交槽位 = Σ(每份已发布/已关闭作业的选课学生数)；
     * 无作业或课程无学生时为 0，不再使用"作业数 × 2"这类魔数口径。
     */
    private Double avgSubmissionRate;

    /** 已提交答卷份数（SUBMITTED / GRADED / REVIEWED） */
    private Integer submittedCount;

    /** 应提交答卷槽位数 = Σ 每份已发布作业的选课学生数 */
    private Integer expectedSubmissionCount;

    /** 已完成批改的答卷份数（GRADED：AI 已评待审 / REVIEWED：教师已终审） */
    private Integer gradedCount;

    /**
     * 班级测验及格率 (0~100)：已有批改成绩的学生中均分 ≥ 60 分的占比。
     * 无任何已批改成绩时为 0（前端展示"暂无样本"）。
     */
    private Integer passRate;

    /** 班级归一平均分 (0~100)：仅统计已批改答卷，无样本为 0 */
    private Double avgScore;

    /**
     * 知识点全班平均掌握度 (0~100)：课程各考点班级平均掌握度的均值。
     * 无考点或无法计算时为 {@code null}，前端展示"暂无数据"。
     */
    private Double knowledgeMasteryAvg;

    /**
     * 掌握度是否包含推算成分：
     * true 表示该课程尚无实测 knowledge_mastery 记录，数值由作业均分推导而来（仅供趋势参考）；
     * false 表示由真实掌握度记录聚合。
     */
    private Boolean masteryEstimated;

    /** 掌握度统计覆盖的学生数 */
    private Integer masteryStudentCount;

    /**
     * AI 辅助批改节约工时（小时）= 已完成批改答卷份数 × 单份人工批改均时。
     * 系数见 {@code TeachingReportServiceImpl.AI_GRADING_MINUTES_PER_PAPER}，属估算值。
     */
    private Double savedHours;

    /** savedHours 是否为估算值（恒为 true，用于前端标注口径，避免被当成实测工时） */
    private Boolean savedHoursEstimated;

    /**
     * 数据更新时间 yyyy-MM-dd HH:mm：统计周期内最近一次真实学习行为时间。
     * 无学习记录时为 {@code null}，前端展示"暂无学习数据"，不得使用浏览器本地时间顶替。
     */
    private String dataUpdatedAt;

    /** 报告是否存在可用于分析的真实数据（有掌握度记录 / 错题记录 / 学习行为） */
    private Boolean hasRealData;

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
        /**
         * 原题题型（SINGLE_CHOICE / MULTIPLE_CHOICE / TRUE_FALSE / FILL_BLANK / SHORT_ANSWER）。
         * 前端据此判断是否需要渲染选项区，避免主观题展示空选项。
         */
        private String questionType;
        /** 原题选项 JSON 字符串（question.options 原样透出，解析规则由前端统一负责） */
        private String questionOptions;
        /** 原题参考答案：用于在选项列表上标注正确项，便于教师讲评 */
        private String questionAnswer;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgePointId;
        private String knowledgePointName;
        private String chapterName;
        private String title;
        /** 该考点累计答错人次（同一考点的多道错题已合并累加） */
        private Integer wrongCount;
        /**
         * 班级掌握度 (0~100)：取自真实 knowledge_mastery 记录聚合值。
         * 无实测记录时为 {@code null}，前端展示"暂无数据"，不再用错误次数公式合成。
         */
        private Integer masteryRate;
        /**
         * 该考点掌握度的实测覆盖人数（有实测记录的学生数，口径为「人」而非「人次」）。
         * 仅 1~2 名学生有实测记录时 0% 并不代表"全班都不掌握"，前端需一并展示样本量。
         */
        private Integer masterySampleCount;
        /**
         * 该考点累计测评次数（knowledge_mastery.sample_count 求和）。
         * 与 {@link #masterySampleCount} 成对展示，教师才能判断百分比覆盖了多少人、多少次测评。
         */
        private Integer masteryAssessmentCount;
        /** 该考点聚合的错题条数：>1 表示同一考点有多道错题，已被合并为一行 */
        private Integer wrongQuestionCount;
        private String errorType;
        private String errorTypeName;
        /**
         * 错因类型是否为关键词推断结果：
         * true 表示 {@code error_types} 未标注、由诊断正文关键词推测，仅供参考，前端需标注"推断"。
         */
        private Boolean errorTypeInferred;
        private String errorReason;
        private String suggestion;
        private String status;
        private String statusLabel;
    }
}
