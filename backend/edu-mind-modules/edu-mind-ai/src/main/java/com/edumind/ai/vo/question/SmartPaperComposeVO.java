package com.edumind.ai.vo.question;

import com.edumind.question.vo.question.QuestionVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SmartPaperComposeVO {
    private List<QuestionVO> questions = new ArrayList<>();
    private Integer selectedCount;
    private Double totalScore;
    private Integer distinctKnowledgePointCount;
    private Double coverageRate;
    private Double duplicateRate = 0.0;
    private Integer shortfallCount = 0;
    private Map<String, Integer> shortfallByDifficulty = new HashMap<>();
    private Map<String, Integer> difficultyHistogram = new HashMap<>();
    private Map<String, Integer> typeDistribution = new HashMap<>();

    /** 本套试卷由真实 AI 原创命题生成的题目数量 */
    private Integer aiGeneratedCount = 0;
    /** 本套试卷从既有题库抽取的题目数量 */
    private Integer bankExtractedCount = 0;
    /** 大模型对本套试卷的质量综合诊断与教学效度评估报告 */
    private String examQualityAssessment;
}
