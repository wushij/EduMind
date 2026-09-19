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
