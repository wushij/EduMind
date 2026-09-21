package com.edumind.ai.dto.question;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class SmartPaperComposeDTO {
    private Long courseId;
    private List<Long> chapterIds;
    private List<Long> knowledgePointIds;
    private Integer totalCount = 10;
    private Integer totalScore = 100;
    private Set<Long> excludeIds;
    private Set<Long> excludeQuestionIds;
    private Map<String, Double> typeRatios;
    private Map<String, Double> difficultyDistribution;
    private List<String> cognitiveLevels;
    /** 难度分布模型标识：FOUNDATION(基础巩固型), NORMAL(标准正态型), ADVANCED(综合拔高型) */
    private String difficultyModel;
    /** 教师个性化命题与组卷提示词指令（如：注重工程实战案例、强化算法推导等） */
    private String promptDirective;
    /** 当题库中题目不足或需全新命题时，是否启用真实 AI 大模型原创命题补全，默认开启 */
    private Boolean aiGenerateFillShortfall = true;
}
