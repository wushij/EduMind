package com.edumind.statistics.vo.analytics;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WrongQuestionAnalyticsVO {
    private List<WrongQuestionItemVO> list = new ArrayList<>();
    private Long total = 0L;

    /** 班级错题总题数 */
    private Long totalWrongQuestions = 0L;
    /** 班级累计错题人次 */
    private Long totalWrongRecords = 0L;
    /** 班级平均错误率 (0.0 ~ 100.0) */
    private Double avgErrorRate = 0.0;
    /** 涉及的薄弱知识点数量 */
    private Integer weakKnowledgePointCount = 0;
    /** 班级已生成变式巩固题总数 */
    private Integer totalVariantQuestions = 0;

    /** 四大错因分布统计: CONCEPT, CALC, LOGIC, READING 对应的人次数量 */
    private Map<String, Integer> errorTypeDistribution = new HashMap<>();

    /** 易错考点 TOP 5 */
    private List<WeakKpSummaryVO> topWeakKnowledgePoints = new ArrayList<>();

    @Data
    public static class WrongQuestionItemVO {
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;

        /** 题干（支持 Markdown / LaTeX 数学公式） */
        private String stem;
        /** 题型代码：SINGLE/MULTIPLE/JUDGE/QA */
        private String type;
        /** 题型展示名：单选题/多选题/判断题/简答题 */
        private String typeName;
        /** 难度：1(简单) - 5(困难) */
        private Integer difficulty;
        /** 选项 JSON 字符串 */
        private String options;
        /** 正确答案 */
        private String answer;
        /** 题目官方解析 */
        private String analysis;

        /** 所属知识点 ID */
        private Long knowledgePointId;
        /** 所属知识点名称 */
        private String knowledgePointName;

        /** 全班做错人数 */
        private Integer wrongStudentCount = 0;
        /** 班级总学生数 */
        private Integer classStudentCount = 0;
        /** 班级错误率 (0.0 ~ 100.0) */
        private Double errorRate = 0.0;
        /** 累计错误次数 */
        private Integer wrongCount = 0;

        /** 错因类型代码列表 (CONCEPT, CALC, LOGIC, READING) */
        private List<String> errorTypes = new ArrayList<>();
        /** 错因类型中文标签 (概念偏差, 计算失误, 逻辑漏洞, 审题偏差) */
        private List<String> errorTypeLabels = new ArrayList<>();

        /** AI 归因诊断结论 */
        private String diagnosis;
        /** 诊断来源：AI=真实大模型生成，LEGACY=预置数据 */
        private String diagnosisSource = "AI";

        /** 关联的变式题 ID 列表 */
        private List<Long> variantQuestionIds = new ArrayList<>();
        /** 变式题数量 */
        private Integer variantCount = 0;

        /** 做错该题的学生作答明细（穿透数据） */
        private List<StudentWrongDetailVO> studentWrongList = new ArrayList<>();
    }

    @Data
    public static class StudentWrongDetailVO {
        private Long studentId;
        private String studentName;
        private String studentNo;
        private String lastStudentAnswer;
        private Integer wrongCount;
        private LocalDateTime updateTime;
    }

    @Data
    public static class WeakKpSummaryVO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Integer wrongCount;
        private Double errorRate;
    }
}

