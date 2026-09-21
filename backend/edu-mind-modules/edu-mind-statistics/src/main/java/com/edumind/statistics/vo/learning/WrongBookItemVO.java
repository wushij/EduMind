package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WrongBookItemVO {
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Integer wrongCount;
    private Integer status;
    private String diagnosis;
    /**
     * 归因结论来源：NONE=暂无结论 / UNANSWERED=未作答不作归因 /
     * LEGACY=演示或历史预置数据（非大模型产出） / AI=大模型实时生成。
     * 前端据此区分「演示假结论」与「AI 真结论」，避免把种子数据误当成模型诊断。
     */
    private String diagnosisSource;
    private List<String> errorTypes = new ArrayList<>();
    private List<String> errorTypeLabels = new ArrayList<>();
    private List<Long> variantQuestionIds = new ArrayList<>();
    private String stem;
    private String type;
    private String difficulty;
    private String options;
    private String answer;
    private String analysis;
    private String studentAnswer;
    private String createTime;
}
