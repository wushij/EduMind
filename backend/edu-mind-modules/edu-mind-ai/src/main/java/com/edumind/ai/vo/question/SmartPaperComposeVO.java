package com.edumind.ai.vo.question;

import com.edumind.question.vo.question.QuestionVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SmartPaperComposeVO {
    private List<QuestionVO> questions = new ArrayList<>();
    private Integer selectedCount;
    private Integer distinctKnowledgePointCount;
    private Double coverageRate;
}
