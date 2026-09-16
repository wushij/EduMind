package com.edumind.teaching.vo.exam;

import com.edumind.question.vo.question.QuestionVO;
import lombok.Data;

@Data
public class ExamQuestionVO {
    private Long questionId;
    private Integer score;
    private Integer sortOrder;
    private QuestionVO question;
}
