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
}
